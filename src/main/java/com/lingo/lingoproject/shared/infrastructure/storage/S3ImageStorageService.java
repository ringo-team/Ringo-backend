package com.lingo.lingoproject.shared.infrastructure.storage;

import com.lingo.lingoproject.shared.domain.model.FeedImage;
import com.lingo.lingoproject.shared.domain.model.PhotographerImage;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.Role;
import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.FeedImageRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PhotographerImageRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ProfileRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.presentation.dto.image.FeedImageDataRequestDto;
import com.lingo.lingoproject.shared.presentation.dto.image.GetFeedImageInfoResponseDto;
import com.lingo.lingoproject.shared.presentation.dto.image.GetImageUrlResponseDto;
import com.lingo.lingoproject.shared.presentation.dto.image.UpdateFeedImageDescriptionRequestDto;
import jakarta.transaction.Transactional;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.Java2DFrameConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.rekognition.RekognitionClient;
import software.amazon.awssdk.services.rekognition.model.CompareFacesMatch;
import software.amazon.awssdk.services.rekognition.model.CompareFacesRequest;
import software.amazon.awssdk.services.rekognition.model.CompareFacesResponse;
import software.amazon.awssdk.services.rekognition.model.DetectFacesRequest;
import software.amazon.awssdk.services.rekognition.model.DetectFacesResponse;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsRequest;
import software.amazon.awssdk.services.rekognition.model.DetectModerationLabelsResponse;
import software.amazon.awssdk.services.rekognition.model.FaceDetail;
import software.amazon.awssdk.services.rekognition.model.Image;
import software.amazon.awssdk.services.rekognition.model.ModerationLabel;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3ImageStorageService {

  private final UserRepository userRepository;
  private final FeedImageRepository feedImageRepository;
  private final PhotographerImageRepository photographerImageRepository;
  private final S3Client amazonS3Client;
  private final ProfileRepository profileRepository;
  private final RekognitionClient amazonRekognition;

  @Value("${aws.s3.bucket}")
  private String bucket;
  @Value("${aws.region.static}")
  private String region;

  private static final int MAX_FEED_IMAGE_COUNT = 9;
  private static final float IMAGE_MODERATION_MIN_CONFIDENCE = 70f;
  private static final float FACE_SIMILARITY_THRESHOLD = 80f;

  // ============================================================
  // Profile image
  // ============================================================

  @Transactional
  public GetImageUrlResponseDto uploadProfileImage(MultipartFile file, Long userId) {
    User user = findUserOrThrow(userId);

    if (profileRepository.existsByUser(user)) {
      log.error("userId={}, step=프로필_중복_업로드", user.getId());
      return null;
    }

    validateProfileImage(file, user);

    String imageUrl = uploadImageToS3(file, "profiles");
    Profile savedProfile = saveProfile(user, imageUrl);
    completeSignupStatus(user);

    log.info("userId={}, profileUrl={}, status={}", userId, savedProfile.getImageUrl(), user.getStatus());

    return new GetImageUrlResponseDto(
        ErrorCode.SUCCESS.getCode(), savedProfile.getImageUrl(), savedProfile.getId());
  }

  public GetImageUrlResponseDto fetchProfileImageUrl(Long userId) {
    User user = findUserOrThrow(userId);
    Profile profile = user.getProfile();
    return new GetImageUrlResponseDto(
        ErrorCode.SUCCESS.getCode(), profile.getImageUrl(), profile.getId());
  }

  @Transactional
  public GetImageUrlResponseDto updateProfileImage(MultipartFile file, Long profileId, Long userId) {
    User user = findUserOrThrow(userId);
    Profile profile = profileRepository.findById(profileId)
        .orElseThrow(() -> new RingoException("프로필을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    validateProfileImage(file, user);
    validateImagePermission(profile.getUser(), userId);

    String oldKey = extractS3ObjectKey(profile.getImageUrl());
    log.info("profileImageKey: {} → {}", profile.getImageUrl(), oldKey);

    deleteS3Object(oldKey);
    String newImageUrl = uploadImageToS3(file, "profiles");

    profile.setImageUrl(newImageUrl);
    profileRepository.save(profile);

    log.info("userId={}, newProfileUrl={}", user.getId(), newImageUrl);

    return new GetImageUrlResponseDto(ErrorCode.SUCCESS.getCode(), newImageUrl, profileId);
  }

  @Transactional
  public void deleteProfileImage(Long profileId, Long userId) {
    Profile profile = profileRepository.findById(profileId)
        .orElseThrow(() -> new RingoException("프로필을 조회할 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    validateImagePermission(profile.getUser(), userId);

    log.info("userId={}, profileId={}, profileUrl={}, deletedAt={}",
        userId, profile.getId(), profile.getImageUrl(), LocalDateTime.now());

    profileRepository.delete(profile);
    deleteS3Object(extractS3ObjectKey(profile.getImageUrl()));
  }

  @Transactional
  public void deleteProfileImageByUser(User user) {
    Profile profile = user.getProfile();
    profileRepository.delete(profile);
    deleteS3Object(profile.getImageUrl());
  }

  // ============================================================
  // Feed image
  // ============================================================

  @Transactional
  public List<GetImageUrlResponseDto> uploadFeedImages(List<FeedImageDataRequestDto> requests, Long userId) {
    User user = findUserOrThrow(userId);

    int existingCount = feedImageRepository.findAllByUser(user).size();
    if (existingCount + requests.size() > MAX_FEED_IMAGE_COUNT) {
      throw new RingoException("최대 업로드 개수를 초과하였습니다.", ErrorCode.OVERFLOW, HttpStatus.BAD_REQUEST);
    }

    List<FeedImage> feedImages = new ArrayList<>();
    for (FeedImageDataRequestDto request : requests) {
      if (containsInappropriateContent(request.getImage())) continue;

      FeedImage feedImage = buildFeedImage(request, user);
      feedImages.add(feedImage);
      log.info("userId={}, imageUrl={}, description={}",
          user.getId(), feedImage.getImageUrl(), feedImage.getDescription());
    }

    log.info("userId={}, uploadedCount={}", user.getId(), feedImages.size());
    List<FeedImage> saved = feedImageRepository.saveAll(feedImages);

    return saved.stream()
        .map(img -> new GetImageUrlResponseDto(
            ErrorCode.SUCCESS.getCode(), img.getImageUrl(), img.getId()))
        .toList();
  }

  public List<GetFeedImageInfoResponseDto> fetchFeedImages(Long userId) {
    User user = findUserOrThrow(userId);
    List<FeedImage> images = feedImageRepository.findAllByUser(user);
    log.info("userId={}, feedImageCount={}", userId, images.size());

    return images.stream()
        .map(img -> new GetFeedImageInfoResponseDto(
            ErrorCode.SUCCESS.getCode(), img.getImageUrl(), img.getId(), img.getDescription()))
        .toList();
  }

  @Transactional
  public GetImageUrlResponseDto updateFeedImage(
      MultipartFile file, Long feedImageId, String description, Long userId
  ) {
    if (containsInappropriateContent(file)) return null;

    FeedImage feedImage = feedImageRepository.findById(feedImageId)
        .orElseThrow(() -> new RingoException("피드 사진을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    validateImagePermission(feedImage.getUser(), userId);

    String oldKey = extractS3ObjectKey(feedImage.getImageUrl());
    log.info("feedImageKey: {} → {}", feedImage.getImageUrl(), oldKey);

    deleteS3Object(oldKey);
    String newImageUrl = uploadImageToS3(file, "feeds");

    feedImage.setImageUrl(newImageUrl);
    feedImage.setDescription(description);
    FeedImage saved = feedImageRepository.save(feedImage);

    log.info("feedImageId={}, imageUrl={}, description={}", saved.getId(), newImageUrl, description);

    return new GetImageUrlResponseDto(ErrorCode.SUCCESS.getCode(), newImageUrl, feedImage.getId());
  }

  @Transactional
  public void deleteFeedImage(Long feedImageId, Long userId) {
    FeedImage feedImage = feedImageRepository.findById(feedImageId)
        .orElseThrow(() -> new RingoException("피드사진을 조회할 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    validateImagePermission(feedImage.getUser(), userId);
    feedImageRepository.delete(feedImage);

    log.info("userId={}, feedImageId={}", userId, feedImageId);

    deleteS3Object(extractS3ObjectKey(feedImage.getImageUrl()));
  }

  @Transactional
  public void deleteAllFeedImagesByUser(User user) {
    List<FeedImage> images = feedImageRepository.findAllByUser(user);
    feedImageRepository.deleteAllByUser(user);

    log.info("userId={}, deletedCount={}", user.getId(), images.size());
    images.forEach(img -> deleteS3Object(img.getImageUrl()));
  }

  public void updateFeedImageDescription(
      UpdateFeedImageDescriptionRequestDto dto, Long feedImageId, Long userId
  ) {
    if (dto == null || dto.description() == null || dto.description().isBlank()) return;

    FeedImage image = feedImageRepository.findById(feedImageId)
        .orElseThrow(() -> new RingoException("피드 이미지를 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    if (!hasImageAccessPermission(image.getUser(), userId)) {
      log.error("authUserId={}, ownerId={}, step=피드_설명_수정_권한_없음", userId, image.getUser().getId());
      throw new RingoException("피드 이미지에 글을 수정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    image.setDescription(dto.description());
    feedImageRepository.save(image);
  }

  // ============================================================
  // Photographer image
  // ============================================================

  @Transactional
  public List<GetImageUrlResponseDto> uploadPhotographerImages(List<MultipartFile> files, Long photographerId) {
    User photographer = findUserOrThrow(photographerId);

    List<PhotographerImage> photographerImages = files.stream()
        .map(file -> PhotographerImage.of(photographer, uploadImageToS3(file, "photographers")))
        .toList();

    return photographerImageRepository.saveAll(photographerImages).stream()
        .map(img -> new GetImageUrlResponseDto(
            ErrorCode.SUCCESS.getCode(), img.getImageUrl(), img.getId()))
        .toList();
  }

  // ============================================================
  // S3 operations
  // ============================================================

  public String uploadImageToS3(MultipartFile file, String folder) {
    String objectKey = buildS3ObjectKey(file, folder);
    try {
      byte[] imageBytes = toImageBytes(file);
      uploadBytesToS3(objectKey, file.getContentType(), imageBytes);
      return buildPublicUrl(objectKey);
    } catch (RingoException e) {
      throw e;
    } catch (Exception e) {
      log.error("S3 업로드 실패. bucket={}, key={}, contentType={}", bucket, objectKey, file.getContentType(), e);
      throw new RingoException("S3 이미지 업로드에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Async
  public void deleteS3Object(String objectKey) {
    try {
      DeleteObjectRequest request = DeleteObjectRequest.builder()
          .bucket(bucket).key(objectKey).build();
      log.info("bucket={}, key={}, step=S3_삭제", bucket, objectKey);
      amazonS3Client.deleteObject(request);
    } catch (Exception e) {
      log.error("S3 삭제 실패. bucket={}, key={}", bucket, objectKey, e);
      throw new RingoException("S3 이미지 삭제에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public String extractS3ObjectKey(String imageUrl) {
    return imageUrl.substring(imageUrl.lastIndexOf("amazonaws.com/") + 14);
  }

  // ============================================================
  // Rekognition — face detection & content moderation
  // ============================================================

  public boolean verifyFaceIdentity(MultipartFile targetImage, User user) {
    try {
      byte[] storedProfileBytes = fetchS3ObjectBytes(
          extractS3ObjectKey(user.getProfile().getImageUrl()));
      return hasFaceMatch(storedProfileBytes, targetImage);
    } catch (Exception e) {
      log.error("userId={}, step=얼굴_인증_실패", user.getId(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean hasFaceMatch(byte[] sourceBytes, MultipartFile targetFile) {
    try {
      byte[] targetBytes = toImageBytes(targetFile);
      Image sourceImage = Image.builder().bytes(SdkBytes.fromByteArray(sourceBytes)).build();
      Image targetImage = Image.builder().bytes(SdkBytes.fromByteArray(targetBytes)).build();

      CompareFacesRequest request = CompareFacesRequest.builder()
          .sourceImage(sourceImage)
          .targetImage(targetImage)
          .similarityThreshold(FACE_SIMILARITY_THRESHOLD)
          .build();

      CompareFacesResponse response = amazonRekognition.compareFaces(request);
      List<CompareFacesMatch> faceMatches = response.faceMatches();

      return !faceMatches.isEmpty();
    } catch (Exception e) {
      log.error("step=얼굴_비교_실패", e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean containsFace(MultipartFile file) {
    Image rekognitionImage;
    try {
      byte[] imageBytes = toImageBytes(file);
      rekognitionImage = Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build();
    } catch (Exception e) {
      log.error("filename={}, step=얼굴_검출_전처리_실패", file.getOriginalFilename(), e);
      throw new RingoException("파일을 바이트로 변환하는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    try {
      DetectFacesRequest request = DetectFacesRequest.builder().image(rekognitionImage).build();
      DetectFacesResponse response = amazonRekognition.detectFaces(request);
      List<FaceDetail> faceDetails = response.faceDetails();
      return !faceDetails.isEmpty();
    } catch (Exception e) {
      log.error("filename={}, step=얼굴_검출_실패", file.getOriginalFilename(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean containsInappropriateContent(MultipartFile file) {
    byte[] imageBytes;
    try {
      imageBytes = toImageBytes(file);
    } catch (Exception e) {
      log.error("filename={}, step=선정성_검사_전처리_실패", file.getOriginalFilename(), e);
      throw new RingoException("선정성 검사 중 파일을 바이트로 변환하지 못하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    try {
      List<ModerationLabel> moderationLabels = detectModerationLabels(imageBytes);
      return moderationLabels.stream().anyMatch(this::isSensitiveLabel);
    } catch (Exception e) {
      log.error("filename={}, step=선정성_검사_실패", file.getOriginalFilename(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  // ============================================================
  // Image format / conversion
  // ============================================================

  public byte[] toImageBytes(MultipartFile file) {
    String extension = extractFileExtension(file);
    log.info("fileExtension={}", extension);
    if (extension.equalsIgnoreCase(".HEIC")) {
      log.info("step=HEIC_to_JPG_변환");
      return convertHeicToJpgBytes(file);
    }
    try {
      return file.getBytes();
    } catch (Exception e) {
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public byte[] convertHeicToJpgBytes(MultipartFile file) {
    try {
      byte[] inputBytes = file.getBytes();
      ByteArrayInputStream inputStream = new ByteArrayInputStream(inputBytes);
      FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream);

      grabber.start();
      Frame frame = grabber.grabImage();

      Java2DFrameConverter converter = new Java2DFrameConverter();
      BufferedImage bufferedImage = converter.convert(frame);

      if (bufferedImage == null) {
        throw new RingoException("프레임을 BufferedImage로 변환하지 못했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
      }

      BufferedImage rgbImage = new BufferedImage(
          bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics = rgbImage.createGraphics();
      graphics.drawImage(bufferedImage, 0, 0, null);
      graphics.dispose();

      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      boolean encoded = ImageIO.write(rgbImage, "jpg", outputStream);
      if (!encoded) {
        throw new RingoException("JPG 인코더를 찾지 못했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
      }

      return outputStream.toByteArray();
    } catch (RingoException e) {
      throw e;
    } catch (Exception e) {
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean hasImageAccessPermission(User imageOwner, Long requestUserId) {
    User requestUser = findUserOrThrow(requestUserId);
    if (requestUser.getRole().equals(Role.ADMIN)) {
      log.info("requestUserId={}, ownerId={}, role=ADMIN, step=관리자_이미지_접근", requestUserId, imageOwner.getId());
      return true;
    }
    return imageOwner.getId().equals(requestUserId);
  }

  public String buildS3ObjectKey(MultipartFile file, String folder) {
    String extension = extractFileExtension(file);
    LocalDate today = LocalDate.now();
    String path = folder + "/" + today.getYear() + "/" +
        today.getYear() + "-" + today.getMonthValue() + "/" + today + "/";
    return path + UUID.randomUUID() + "_image" + extension;
  }

  public String extractFileExtension(MultipartFile file) {
    String filename = file.getOriginalFilename();
    if (filename != null && filename.contains(".")) {
      String extension = filename.substring(filename.lastIndexOf("."));
      if (extension.equalsIgnoreCase(".HEIC")) return ".jpg";
      return extension;
    }
    throw new RingoException("파일의 확장자가 없습니다.", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
  }

  // ============================================================
  // Private helpers
  // ============================================================

  private void uploadBytesToS3(String objectKey, String contentType, byte[] imageBytes) {
    PutObjectRequest putRequest = PutObjectRequest.builder()
        .bucket(bucket)
        .key(objectKey)
        .contentType(contentType)
        .contentLength((long) imageBytes.length)
        .build();

    log.info("bucket={}, key={}, contentType={}, size={}",
        bucket, objectKey, contentType, imageBytes.length);

    amazonS3Client.putObject(
        putRequest,
        RequestBody.fromInputStream(new ByteArrayInputStream(imageBytes), imageBytes.length));
  }

  private String buildPublicUrl(String objectKey) {
    String url = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + objectKey;
    log.info("objectKey={} → publicUrl={}", objectKey, url);
    return url;
  }

  private byte[] fetchS3ObjectBytes(String s3Key) throws Exception {
    GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(s3Key).build();
    ResponseInputStream<GetObjectResponse> stream = amazonS3Client.getObject(request);
    return stream.readAllBytes();
  }

  private List<ModerationLabel> detectModerationLabels(byte[] imageBytes) {
    DetectModerationLabelsRequest request = DetectModerationLabelsRequest.builder()
        .image(Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build())
        .minConfidence(IMAGE_MODERATION_MIN_CONFIDENCE)
        .build();
    DetectModerationLabelsResponse response = amazonRekognition.detectModerationLabels(request);
    return response.moderationLabels();
  }

  private boolean isSensitiveLabel(ModerationLabel label) {
    if (label.confidence() < IMAGE_MODERATION_MIN_CONFIDENCE) return false;
    return matchesSensitiveCategory(label.name())
        || matchesSensitiveCategory(label.parentName());
  }

  private boolean matchesSensitiveCategory(String category) {
    return category != null && (
        category.contains("Nudity")
            || category.contains("Sexual")
            || category.contains("Suggestive"));
  }

  private void validateProfileImage(MultipartFile file, User user) {
    if (!containsFace(file)) {
      log.warn("userId={}, step=얼굴_없는_프로필_업로드", user.getId());
      throw new RingoException("프로필에 얼굴이 존재하지 않습니다.", ErrorCode.FACE_NOT_FOUND, HttpStatus.NOT_ACCEPTABLE);
    }
    if (containsInappropriateContent(file)) {
      log.error("userId={}, step=부적절한_이미지_업로드", user.getId());
      throw new RingoException("적절하지 않은 사진을 업로드 하였습니다.", ErrorCode.UNMODERATE, HttpStatus.NOT_ACCEPTABLE);
    }
  }

  private void validateImagePermission(User imageOwner, Long requestUserId) {
    if (!hasImageAccessPermission(imageOwner, requestUserId)) {
      log.error("requestUserId={}, ownerId={}, step=이미지_권한_없음", requestUserId, imageOwner.getId());
      throw new RingoException("이미지를 업데이트할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }
  }

  private Profile saveProfile(User user, String imageUrl) {
    Profile profile = Profile.of(user, imageUrl);
    Profile saved = profileRepository.save(profile);
    user.setProfile(saved);
    userRepository.save(user);
    return saved;
  }

  private void completeSignupStatus(User user) {
    user.setStatus(SignupStatus.COMPLETED);
    userRepository.save(user);
  }

  private FeedImage buildFeedImage(FeedImageDataRequestDto dto, User user) {
    String imageUrl = uploadImageToS3(dto.getImage(), "feeds");
    return FeedImage.of(user, imageUrl, dto.getContent());
  }

  private User findUserOrThrow(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new RingoException(
            "유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));
  }
}
