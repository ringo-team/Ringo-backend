package com.lingo.lingoproject.api.image;


import com.lingo.lingoproject.db.domain.FeedImage;
import com.lingo.lingoproject.db.domain.PhotographerImage;
import com.lingo.lingoproject.db.domain.Profile;
import com.lingo.lingoproject.db.domain.User;
import com.lingo.lingoproject.db.domain.enums.Role;
import com.lingo.lingoproject.db.domain.enums.SignupStatus;
import com.lingo.lingoproject.common.exception.ErrorCode;
import com.lingo.lingoproject.common.exception.RingoException;
import com.lingo.lingoproject.api.image.dto.FeedImageDataRequestDto;
import com.lingo.lingoproject.api.image.dto.GetFeedImageInfoResponseDto;
import com.lingo.lingoproject.api.image.dto.GetImageUrlResponseDto;
import com.lingo.lingoproject.api.image.dto.UpdateFeedImageDescriptionRequestDto;
import com.lingo.lingoproject.db.repository.PhotographerImageRepository;
import com.lingo.lingoproject.db.repository.ProfileRepository;
import com.lingo.lingoproject.db.repository.FeedImageRepository;
import com.lingo.lingoproject.db.repository.UserRepository;
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
public class ImageService {

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

  private final Integer MAX_NUMBER_OF_SNAP_IMAGES = 9;
  private final Float IMAGE_MODERATION_MIN_CONFIDENCE = 70f;
  private final Float FACE_SIMILARITY_THRESHOLD = 80F;


  /**
   * profile 이미지 crud
   */
  @Transactional
  public GetImageUrlResponseDto uploadProfileImage(MultipartFile file, Long  userId) {

    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("프로필을 업로드하던 중 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));

    // 이미 프로필 사진이 존재할 경우 업로드 할 수 없다.
    if (profileRepository.existsByUser(user)){
      log.error("""
          
          userId: {} 의 프로필 두번 요청되었습니다.
          
          """, user.getId());
      return null;
    }

    // s3에 이미지 업로드
    String imageUrl = uploadImageToS3(file, "profiles");

    // url db에 저장
    Profile profile = Profile.builder()
        .user(user)
        .imageUrl(imageUrl)
        .build();

    Profile savedProfile = profileRepository.save(profile);

    user.setProfile(savedProfile);
    userRepository.save(user);

    // 회원가입을 성공적으로 마무리 했으므로 user status를 COMPLETE로 변경한다.
    alterSignupStatusToComplete(user);

    log.info("""
        
        userId: {},
        profileUrl: {},
        status: {} -----> {}
        
        """,
        userId,
        profile.getImageUrl(),
        SignupStatus.BEFORE,
        user.getStatus()
    );

    return new GetImageUrlResponseDto(
        ErrorCode.SUCCESS.getCode(),
        savedProfile.getImageUrl(),
        savedProfile.getId()
    );
  }

  private void alterSignupStatusToComplete(User user){
    user.setStatus(SignupStatus.COMPLETED);
    userRepository.save(user);
  }

  public GetImageUrlResponseDto getProfileImageUrl(Long userId){
    User user = userRepository.findById(userId).orElseThrow(
        () -> new RingoException("유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST)
    );
    Profile profile = user.getProfile();
    return new GetImageUrlResponseDto(
        ErrorCode.SUCCESS.getCode(),
        profile.getImageUrl(),
        profile.getId()
    );
  }

  @Transactional
  public GetImageUrlResponseDto updateProfileImage(MultipartFile file, Long profileId, Long userId){

    User user = userRepository.findById(userId).orElseThrow(
        () -> new RingoException("프로필을 업로드하던 중 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST)
    );
    Profile profile = profileRepository.findById(profileId).orElseThrow(
        () -> new RingoException("프로필을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );

    validateProfileImage(file, user);

    // 업데이트 권한 확인
    checkImagePermission(profile.getUser(), user.getId());

    // 업데이트 key 얻기
    String imageUrl = getFilenameFromS3ImageUrl(profile.getImageUrl());

    log.info("""
        
        imageUrl 변환 : {} ------> {}
        
        """, profile.getImageUrl(), imageUrl);

    // s3에 이미지 삭제
    deleteImageInS3(imageUrl);

    // s3에 새로운 이미지 파일 업로드
    imageUrl = uploadImageToS3(file, "profiles");

    // profiles 테이블에 변경된 이미지 url 저장
    profile.setImageUrl(imageUrl);
    profileRepository.save(profile);

    log.info("""
        
        userId: {},
        profileUrl: {}
        프로필 수정 완료
        
        """, user.getId(), profile.getImageUrl());

    return new GetImageUrlResponseDto(
        ErrorCode.SUCCESS.getCode(),
        imageUrl,
        profileId
    );
  }

  private void validateProfileImage(MultipartFile file, User user){
    if (!existsFaceInImage(file)){
      log.warn("""
          
          userId: {}, 얼굴이 존재하지 않은 프로필을 업로드 하였습니다.
          
          """, user.getId());
      throw new RingoException("프로필에 얼굴이 존재하지 않습니다.", ErrorCode.FACE_NOT_FOUND, HttpStatus.NOT_ACCEPTABLE);
    }

    if (isUnmoderateImage(file)){
      log.error("""

          userId={},
          step=부적절한_이미지_업로드,
          status=FAILED

          """, user.getId());
      throw new RingoException("적절하지 않은 사진을 업로드 하였습니다.", ErrorCode.UNMODERATE, HttpStatus.NOT_ACCEPTABLE);
    }
  }

  private void checkImagePermission(User host, Long userId){
    if(!hasPermissionOnImage(host, userId)){
      log.error("""

          authUserId={},
          userId={},
          step=잘못된_유저_요청,
          status=FAILED,
          이미지의 권한이 없는 유저가 접근하였습니다.

          """, userId, host.getId());
      throw new RingoException("이미지를 업데이트할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }
  }

  @Transactional
  public List<GetImageUrlResponseDto> uploadFeedImages(List<FeedImageDataRequestDto> list, Long userId) {

    User user = userRepository.findById(userId).orElseThrow(() ->
        new RingoException("유저를 찾을 수 없습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST));

    List<FeedImage> existedFeedImages = feedImageRepository.findAllByUser(user);
    if (existedFeedImages.size() + list.size() > MAX_NUMBER_OF_SNAP_IMAGES){
      throw new RingoException("최대 업로드 개수를 초과하였습니다.", ErrorCode.OVERFLOW, HttpStatus.BAD_REQUEST);
    }

    List<FeedImage> feedImages = new ArrayList<>();

    for(FeedImageDataRequestDto file : list){

      if (isUnmoderateImage(file.getImage())){
        continue;
      }

      String imageUrl = uploadImageToS3(file.getImage(), "feeds");

      FeedImage feedImage = FeedImage.builder()
          .imageUrl(imageUrl)
          .description(file.getContent())
          .user(user)
          .build();
      feedImages.add(feedImage);
      log.info("""
        
        userId: {},
        imageUrl: {},
        description: {}
        
        """,
          user.getId(),
          feedImage.getImageUrl(),
          feedImage.getDescription()
      );
    }
    log.info("""
        
        userId: {},
        피드_이미지_개수: {} 개의 이미지를 업로드하였습니다.
        
        """, user.getId(), feedImages.size());

    List<FeedImage> savedFeedImages = feedImageRepository.saveAll(feedImages);

    return savedFeedImages.stream()
        .map(image ->
            new GetImageUrlResponseDto(
                ErrorCode.SUCCESS.getCode(),
                image.getImageUrl(),
                image.getId()
            )
        )
        .toList();
  }

  @Transactional
  public void deleteProfile(Long profileId, Long userId){
    // 프로필 조회
    Profile profile = profileRepository.findById(profileId).orElseThrow(() -> new RingoException(
        "프로필을 조회할 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    // 삭제 권한 확인
    checkImagePermission(profile.getUser(), userId);

    log.info("""
        
        user_id: {},
        profile_id: {},
        profile_url: {},
        삭제_시간: {}
        
        """,
        userId,
        profile.getId(),
        profile.getImageUrl(),
        LocalDateTime.now());

    // db 삭제
    profileRepository.delete(profile);

    // 키 조회 및 s3 삭제
    String imageUrl = getFilenameFromS3ImageUrl(profile.getImageUrl());

    log.info("""
        
        imageUrl 변환 : {} ------> {}
        
        """, profile.getImageUrl(), imageUrl);

    deleteImageInS3(imageUrl);
  }

  @Transactional
  public void deleteProfileImageByUser(User user){

    Profile profile = user.getProfile();

    profileRepository.delete(profile);

    deleteImageInS3(profile.getImageUrl());
  }

  /**
   * feed 이미지 crud
   */
  public List<GetFeedImageInfoResponseDto> getAllFeedImageUrls(Long userId){
    User user = userRepository.findById(userId).orElseThrow(
        () -> new RingoException("유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST)
    );
    List<FeedImage> images = feedImageRepository.findAllByUser(user);
    log.info("""
        
        userId: {},
        피드_이미지_개수: {} 개의 이미지를 조회했습니다.
        
        """, userId, images.size());
    return images.stream()
        .map(image ->
            new GetFeedImageInfoResponseDto(
                ErrorCode.SUCCESS.getCode(),
                image.getImageUrl(),
                image.getId(),
                image.getDescription()
            )
        )
        .toList();
  }

  @Transactional
  public GetImageUrlResponseDto updateFeedImage(MultipartFile file, Long snapImageId, String description, Long userId){

    if (isUnmoderateImage(file)){
      return null;
    }

    FeedImage feedImage = feedImageRepository.findById(snapImageId).orElseThrow(() -> new RingoException(
        "피드 사진을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    checkImagePermission(feedImage.getUser(), userId);

    String imageUrl = getFilenameFromS3ImageUrl(feedImage.getImageUrl());

    log.info("""
        
        imageUrl 변환 : {} ------> {}
        
        """, feedImage.getImageUrl(), imageUrl);

    //s3에 이미지 삭제
    deleteImageInS3(imageUrl);

    // s3에 새로운 이미지 업로드
    imageUrl = uploadImageToS3(file, "feeds");

    // snap_images 테이블에 변경된 사진 url 저장
    feedImage.setImageUrl(imageUrl);
    feedImage.setDescription(description);
    FeedImage savedFeedImage = feedImageRepository.save(feedImage);

    log.info("""
        
        feed-image-id: {},
        image-url: {},
        description: {}
        
        """,
        savedFeedImage.getId(),
        imageUrl,
        description
        );

    return new GetImageUrlResponseDto(
        ErrorCode.SUCCESS.getCode(),
        imageUrl,
        feedImage.getId()
    );
  }

  @Transactional
  public void deleteFeedImage(Long snapImageId, Long userId){
    // 피드 사진 조회
    FeedImage feedImage = feedImageRepository.findById(snapImageId).orElseThrow(() -> new RingoException(
        "피드사진을 조회할 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    // 삭제 권한 확인
    checkImagePermission(feedImage.getUser(), userId);

    // db 값 삭제
    feedImageRepository.delete(feedImage);

    log.info("""
        
        userId: {} 가 피드_사진_id {} 를 삭제하였습니다.
        
        """, userId, snapImageId);

    // 키 조회 및 s3 값 삭제
    String imageUrl = getFilenameFromS3ImageUrl(feedImage.getImageUrl());
    deleteImageInS3(imageUrl);
  }

  @Transactional
  public void deleteAllFeedImagesByUser(User user){
    List<FeedImage> images = feedImageRepository.findAllByUser(user);
    feedImageRepository.deleteAllByUser(user);

    log.info("""
        
        user-id: {},
        삭제할_이미지_개수: {} 개를 삭제하였습니다.
        
        """,
        user.getId(),
        images.size()
        );

    for(FeedImage feedImage : images){
      deleteImageInS3(feedImage.getImageUrl());
    }
  }

  /**
   * photographer 이미지 crud
   */
  @Transactional
  public List<GetImageUrlResponseDto> uploadPhotographerExampleImages(List<MultipartFile> images, Long photographerId){
    User photographer = userRepository.findById(photographerId).orElseThrow(() -> new RingoException(
        "유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));

    List<PhotographerImage> photographerImages = new ArrayList<>();

    for (MultipartFile file : images){
      String imageUrl = uploadImageToS3(file, "photographers");
      PhotographerImage photographerImage = PhotographerImage.builder()
          .imageUrl(imageUrl)
          .photographer(photographer)
          .build();
      photographerImages.add(photographerImage);
    }
    List<PhotographerImage> savedPhotographerImages = photographerImageRepository.saveAll(photographerImages);
    return savedPhotographerImages.stream()
        .map(image ->
            new GetImageUrlResponseDto(
                ErrorCode.SUCCESS.getCode(),
                image.getImageUrl(),
                image.getId()
            )
        )
        .toList();
  }

  public  String uploadImageToS3(MultipartFile file, String type){

    String filename = getFilenameByAppendFilePath(file, type);

    try {
      byte[] imageByte = checkImageFormatAndGetAppropriateImageByte(file);

      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucket)
          .key(filename)
          .contentType(file.getContentType())
          .contentLength((long) imageByte.length)
          .build();

      log.info("""
          
          bucket: {},
          filename: {},
          content-type: {},
          content-length: {}
          이미지를 해당 bucket에 업로드 합니다.
          
          """,
          bucket,
          filename,
          file.getContentType(),
          (long) imageByte.length
      );

      // S3 업로드
      amazonS3Client.putObject(
          putObjectRequest,
          RequestBody.fromInputStream(new ByteArrayInputStream(imageByte), file.getSize())
      );

      String filePath = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + filename;

      log.info("""
          
          파일 path 변환 : {} ----> {}
          
          """, filename, filePath);

      return filePath;
    }catch (Exception e){
      log.error("S3 이미지 업로드 실패. bucket: {}, filename: {}, contentType: {}", bucket, filename, file.getContentType(), e);
      throw new RingoException("s3에 이미지 업로드 하는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public String getFilenameByAppendFilePath(MultipartFile file, String type){

    String imageFormat = getImageFileFormatContainingDot(file);

    LocalDate now = LocalDate.now();

    String path = type + "/" + now.getYear() + "/" +
        now.getYear() + "-" + now.getMonthValue() + "/" +
        now + "/";

     return path + UUID.randomUUID() + "_image" + imageFormat;
  }

  public String getImageFileFormatContainingDot(MultipartFile file){
    String imageFormat = file.getOriginalFilename();
    if(imageFormat != null && imageFormat.contains(".")){
      imageFormat = imageFormat.substring(imageFormat.lastIndexOf("."));
      if (imageFormat.equalsIgnoreCase(".HEIC")) return ".jpg";
      return imageFormat;
    }
    throw new RingoException("파일의 확장자가 없습니다.", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
  }

  public boolean hasPermissionOnImage(User user, Long accessUserId){
    Long userId = user.getId();
    User accessUser = userRepository.findById(accessUserId).orElseThrow(() -> new RingoException(
        "Token not found", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));
    if(accessUser.getRole().equals(Role.ADMIN)){
      log.info("""
          
          accessUserId: {} | accessUserNickname: {} | permission: {}
          admin이 user_id: {}, 사진 권한에 접근하였습니다.
          
          """,
          accessUser.getId(),
          accessUser.getNickname(),
          Role.ADMIN,
          user.getId()
      );
      return true;
    }
    return userId.equals(accessUserId);
  }

  @Async
  public void deleteImageInS3(String filename){
    try {
      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(filename)
          .build();
      log.info("""
          
          bucket: {},
          filename: {},
          파일을 s3에서 삭제합니다.
          
          """,
          bucket,
          filename
          );
      amazonS3Client.deleteObject(deleteObjectRequest);
    }catch (Exception e){
      log.error("s3에 이미지를 삭제하는데 실패하였습니다. bucket: {}, image_url: {}", bucket, filename, e);
      throw new RingoException("s3에 이미지를 삭제하는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public String getFilenameFromS3ImageUrl(String ImageUrl){
    return  ImageUrl.substring(ImageUrl.lastIndexOf("amazonaws.com/") + 14);
  }

  public void updateFeedImageDescription(UpdateFeedImageDescriptionRequestDto dto, Long snapImageId, Long userId){

    if (dto == null || dto.description() == null || dto.description().isBlank()){
      return;
    }

    FeedImage image = feedImageRepository.findById(snapImageId)
        .orElseThrow(() -> new RingoException("피드 이미지를 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    if (!hasPermissionOnImage(image.getUser(), userId)){
      log.error("""

          authUserId={},
          userId={},
          step=잘못된_유저_요청,
          status=FAILED

          """, userId, image.getUser().getId());
      throw new RingoException("피드 이미지에 글을 수정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    image.setDescription(dto.description());
    feedImageRepository.save(image);
  }

  public boolean isUnmoderateImage(MultipartFile image) {
    byte[] imageBytes;
    try {
      imageBytes = checkImageFormatAndGetAppropriateImageByte(image);
    }catch (Exception e){
      log.error("선정성 검사 중 이미지를 바이트로 변환하는데 실패하였습니다. filename: {}", image.getOriginalFilename(), e);
      throw new RingoException("선정성 검사 중 파일을 바이트로 변환하지 못하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    try {
      DetectModerationLabelsRequest request = DetectModerationLabelsRequest.builder()
          .image(Image.builder()
              .bytes(SdkBytes.fromByteArray(imageBytes))
              .build())
          .minConfidence(IMAGE_MODERATION_MIN_CONFIDENCE)
          .build();

      DetectModerationLabelsResponse response = amazonRekognition.detectModerationLabels(request);

      List<ModerationLabel> labels = response.moderationLabels();

      return labels.stream().anyMatch(label -> {
        String name = label.name();
        String parent = label.parentName();

        if (label.confidence() < IMAGE_MODERATION_MIN_CONFIDENCE)
          return false;

        return (name != null && (name.contains("Nudity") || name.contains("Sexual") || name.contains("Suggestive")))
            || (parent != null && (parent.contains("Nudity") || parent.contains("Sexual") || parent.contains("Suggestive")));
      });
    }catch (Exception e){
      log.error("이미지의 선정성 검사를 하는데 실패하였습니다. filename: {}", image.getOriginalFilename(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean verifyProfileImage(MultipartFile targetImage, User user){
    try {
      String profileUrl = getFilenameFromS3ImageUrl(user.getProfile().getImageUrl());
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(profileUrl)
          .build();
      ResponseInputStream<GetObjectResponse> objectStream = amazonS3Client.getObject(getObjectRequest);

      return isSamePerson(objectStream.readAllBytes(), targetImage);
    }catch (Exception e){
      log.error("프로필 사진을 인증하는데 실패하였습니다. userId: {}", user.getId(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean isSamePerson(byte[] source, MultipartFile target){
    try{

      byte[] targetImageByte = checkImageFormatAndGetAppropriateImageByte(target);

      Image sourceImage = Image.builder().bytes(SdkBytes.fromByteArray(source)).build();
      Image targetImage = Image.builder().bytes(SdkBytes.fromByteArray(targetImageByte)).build();

      CompareFacesRequest request = CompareFacesRequest.builder()
          .sourceImage(sourceImage)
          .targetImage(targetImage)
          .similarityThreshold(FACE_SIMILARITY_THRESHOLD)
          .build();

      CompareFacesResponse response = amazonRekognition.compareFaces(request);
      List<CompareFacesMatch> matches = response.faceMatches();

      if (matches.isEmpty()){
        return false;
      }

      return true;

    }catch (Exception e){
      log.error("이미지 인증을 하는데 실패하였습니다.", e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean existsFaceInImage(MultipartFile targetImage){
    Image image;
    try {
      byte[] imageBytes = checkImageFormatAndGetAppropriateImageByte(targetImage);
      image = Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build();
    }catch (Exception e){
      log.error("이미지의 얼굴 여부 검수 중 파일을 바이트로 변환하는데 실패하였습니다. filename: {}", targetImage.getOriginalFilename(), e);
      throw new RingoException("파일을 바이트로 변환하는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    try {
      DetectFacesRequest request = DetectFacesRequest.builder().image(image).build();
      DetectFacesResponse result = amazonRekognition.detectFaces(request);
      List<FaceDetail> faceDetails = result.faceDetails();
      return !faceDetails.isEmpty();
    }catch (Exception e){
      log.error("이미지의 얼굴 존재여부를 검수하는데 실패하였습니다. filename: {}", targetImage.getOriginalFilename(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public byte[] checkImageFormatAndGetAppropriateImageByte(MultipartFile image){
    String imageFormat = getImageFileFormatContainingDot(image);
    log.info("파일 확장자: {}", imageFormat);
    if (imageFormat.equalsIgnoreCase(".HEIC")){
      log.info("파일 확장자 .HEIC -> .JPG 변경");
      return convertHEICToJPG(image);
    }
    try {
      return image.getBytes();
    }catch (Exception e){
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public byte[] convertHEICToJPG(MultipartFile image){
    try{
      byte[] inputByte = image.getBytes();
      ByteArrayInputStream bais = new ByteArrayInputStream(inputByte);
      FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(bais);

      grabber.start();
      Frame frame = grabber.grabImage();

      Java2DFrameConverter converter = new Java2DFrameConverter();
      BufferedImage bufferedImage = converter.convert(frame);

      if (bufferedImage ==  null){
        throw new RingoException("frame을 buffer image로 변환하지 못함", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
      }

      BufferedImage rgb = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D graphics2D = rgb.createGraphics();
      graphics2D.drawImage(bufferedImage, 0, 0, null);
      graphics2D.dispose();

      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      boolean ok = ImageIO.write(rgb, "jpg", baos);
      if (!ok) throw new RingoException("JPG encoder를 찾지 못함", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);

      return baos.toByteArray();


    } catch (Exception e) {
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
