package com.lingo.lingoproject.image;


import com.lingo.lingoproject.domain.PhotographerImage;
import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.SnapImage;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.domain.enums.SignupStatus;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.image.dto.GetImageUrlResponseDto;
import com.lingo.lingoproject.image.dto.UpdateSnapImageDescriptionRequestDto;
import com.lingo.lingoproject.repository.PhotographerImageRepository;
import com.lingo.lingoproject.repository.ProfileRepository;
import com.lingo.lingoproject.repository.SnapImageRepository;
import com.lingo.lingoproject.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final SnapImageRepository snapImageRepository;
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
  public GetImageUrlResponseDto uploadProfileImage(MultipartFile file, User  user) {

    if (!existsFaceInImage(file)){
      throw new RingoException("프로필에 얼굴이 존재하지 않습니다.", HttpStatus.NOT_ACCEPTABLE);
    }

    if (isUnmoderateImage(file)){
      log.error("userId={}, step=부적절한_이미지_업로드, status=FAILED", user.getId());
      throw new RingoException("적절하지 않은 사진을 업로드 하였습니다.", HttpStatus.NOT_ACCEPTABLE);
    }

    // 이미 프로필 사진이 존재할 경우 업로드 할 수 없다.
    if (profileRepository.existsByUser(user)){
      return null;
    }

    String imageUrl = uploadImageToS3(file, "profiles");

    Profile profile = Profile.builder()
        .user(user)
        .imageUrl(imageUrl)
        .build();

    Profile savedProfile = profileRepository.save(profile);

    // 회원가입을 성공적으로 마무리 했으므로 user status를 COMPLETE로 변경한다.
    user.setStatus(SignupStatus.COMPLETED);
    userRepository.save(user);

    return new GetImageUrlResponseDto(savedProfile.getImageUrl(), savedProfile.getId());
  }

  public GetImageUrlResponseDto getProfileImageUrl(Long userId){
    User user = userRepository.findById(userId).orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    Profile profile = profileRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("유저가 프로필을 가지지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
    return new GetImageUrlResponseDto(profile.getImageUrl(), profile.getId());
  }

  @Transactional
  public GetImageUrlResponseDto updateProfileImage(MultipartFile file, Long profileId, Long userId){

    // 프로필은 얼굴이 나와야함
    if (!existsFaceInImage(file)){
      throw new RingoException("프로필에 얼굴이 존재하지 않습니다.", HttpStatus.NOT_ACCEPTABLE);
    }

    // 선정적인 사진은 업로드 하지 못함
    if (isUnmoderateImage(file)){
      log.error("userId={}, step=부적절한_이미지_업로드, status=FAILED", userId);
      throw new RingoException("적절하지 않은 사진을 업로드 하였습니다.", HttpStatus.NOT_ACCEPTABLE);
    }

    // 프로필 조회
    Profile profile = profileRepository.findById(profileId).orElseThrow(() -> new RingoException("프로필을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    // 업데이트 권한 확인
    if(!hasPermissionOnImage(profile.getUser(), userId)){
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", userId, profile.getUser().getId());
      throw new RingoException("이미지를 업데이트할 권한이 없습니다.", HttpStatus.BAD_REQUEST);
    }

    // 업데이트 key 얻기
    String imageUrl = getOriginalFilename(profile.getImageUrl());

    // s3에 이미지 삭제
    deleteImageInS3(imageUrl);

    // s3에 새로운 이미지 파일 업로드
    imageUrl = uploadImageToS3(file, "profiles");

    // profiles 테이블에 변경된 이미지 url 저장
    profile.setImageUrl(imageUrl);
    profileRepository.save(profile);
    return new GetImageUrlResponseDto(imageUrl, profileId);
  }

  @Transactional
  public List<GetImageUrlResponseDto> uploadSnapImages(List<MultipartFile> images, User user) {

    List<SnapImage> existedSnapImages = snapImageRepository.findAllByUser(user);
    if (existedSnapImages.size() + images.size() > MAX_NUMBER_OF_SNAP_IMAGES){
      throw new RingoException("최대 업로드 개수를 초과하였습니다.", HttpStatus.BAD_REQUEST);
    }

    List<SnapImage> snapImages = new ArrayList<>();

    for(MultipartFile file : images){

      if (isUnmoderateImage(file)){
        continue;
      }

      String imageUrl = uploadImageToS3(file, "snaps");
      SnapImage snapImage = SnapImage.builder()
          .imageUrl(imageUrl)
          .user(user)
          .build();
      snapImages.add(snapImage);
    }
    List<SnapImage> savedSnapImages = snapImageRepository.saveAll(snapImages);

    return savedSnapImages.stream()
        .map(image -> {
          return new GetImageUrlResponseDto(image.getImageUrl(), image.getId());
        })
        .toList();
  }

  @Transactional
  public void deleteProfile(Long profileId, Long userId){
    // 프로필 조회
    Profile profile = profileRepository.findById(profileId).orElseThrow(() -> new RingoException("프로필을 조회할 수 없습니다.", HttpStatus.BAD_REQUEST));

    // 삭제 권한 확인
    if(!hasPermissionOnImage(profile.getUser(), userId)){
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", userId, profile.getUser().getId());
      throw new RingoException("프로필을 삭제할 권한이 없습니다.", HttpStatus.BAD_REQUEST);
    }
    // db 삭제
    profileRepository.delete(profile);

    // 키 조회 및 s3 삭제
    String imageUrl = getOriginalFilename(profile.getImageUrl());
    deleteImageInS3(imageUrl);
  }

  @Transactional
  public void deleteProfileImageByUser(User user){
    Profile profile = profileRepository.findByUser(user).orElseThrow(() -> new RingoException("프로필을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    profileRepository.delete(profile);

    deleteImageInS3(profile.getImageUrl());
  }

  /**
   * snap 이미지 crud
   */
  public List<GetImageUrlResponseDto> getAllSnapImageUrls(Long userId){
    User user = userRepository.findById(userId).orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    List<SnapImage> images = snapImageRepository.findAllByUser(user);
    return images.stream()
        .map(image -> new GetImageUrlResponseDto(image.getImageUrl(), image.getId()))
        .toList();
  }

  @Transactional
  public GetImageUrlResponseDto updateSnapImage(MultipartFile file, Long snapImageId, String description, Long userId){

    if (isUnmoderateImage(file)){
      return null;
    }

    SnapImage snapImage = snapImageRepository.findById(snapImageId).orElseThrow(() -> new RingoException("스냅 사진을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    if(!hasPermissionOnImage(snapImage.getUser(), userId)){
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", userId, snapImage.getUser().getId());
      throw new RingoException("업데이트 할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
    String imageUrl = getOriginalFilename(snapImage.getImageUrl());

    //s3에 이미지 삭제
    deleteImageInS3(imageUrl);

    // s3에 새로운 이미지 업로드
    imageUrl = uploadImageToS3(file, "snaps");

    // snap_images 테이블에 변경된 사진 url 저장
    snapImage.setImageUrl(imageUrl);
    snapImage.setDescription(description);
    snapImageRepository.save(snapImage);

    return new GetImageUrlResponseDto(imageUrl, snapImage.getId());
  }

  @Transactional
  public void deleteSnapImage(Long snapImageId, Long userId){
    // 스냅 사진 조회
    SnapImage snapImage = snapImageRepository.findById(snapImageId).orElseThrow(() -> new RingoException("스냅사진을 조회할 수 없습니다.", HttpStatus.BAD_REQUEST));

    // 삭제 권한 확인
    if (!hasPermissionOnImage(snapImage.getUser(), userId)){
      return;
    }

    // db 값 삭제
    snapImageRepository.delete(snapImage);

    // 키 조회 및 s3 값 삭제
    String imageUrl = getOriginalFilename(snapImage.getImageUrl());
    deleteImageInS3(imageUrl);
  }

  @Transactional
  public void deleteAllSnapImagesByUser(User user){
    List<SnapImage> images = snapImageRepository.findAllByUser(user);
    snapImageRepository.deleteAllByUser(user);

    for(SnapImage snapImage : images){
      deleteImageInS3(snapImage.getImageUrl());
    }
  }

  /**
   * photographer 이미지 crud
   */

  @Transactional
  public List<GetImageUrlResponseDto> uploadPhotographerExampleImages(List<MultipartFile> images, Long photographerId){
    User photographer = userRepository.findById(photographerId).orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

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
        .map(image -> {
          return new GetImageUrlResponseDto(image.getImageUrl(), image.getId());
        })
        .toList();
  }

  public  String uploadImageToS3(MultipartFile file, String type){
    String originalFilename = file.getOriginalFilename();
    if(originalFilename != null && originalFilename.contains(".")){
      originalFilename = originalFilename.substring(originalFilename.lastIndexOf("."));
    }

    LocalDate now = LocalDate.now();
    String path = type + "/" + now.getYear() + "/" +
                  now.getYear() + "-" + now.getMonthValue() + "/" +
                  now + "/";

    String filename = path + UUID.randomUUID().toString() + "_image_" + originalFilename;

    String imageUrl = null;

    try {
      PutObjectRequest putObjectRequest = PutObjectRequest.builder()
          .bucket(bucket)
          .key(filename)
          .contentType(file.getContentType())
          .contentLength(file.getSize())
          .build();

      // S3 업로드
      amazonS3Client.putObject(
          putObjectRequest,
          RequestBody.fromInputStream(file.getInputStream(), file.getSize())
      );

      imageUrl = "https://" + bucket + ".s3." + region + ".amazonaws.com/" + filename;
    }catch (Exception e){
      log.error("S3 이미지 업로드 실패. bucket: {}, filename: {}, contentType: {}", bucket, filename, file.getContentType(), e);
      throw new RingoException("s3에 이미지 업로드 하는데 실패하였습니다.",  HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return imageUrl;
  }

  public boolean hasPermissionOnImage(User user, Long accessUserId){
    Long userId = user.getId();
    User accessUser = userRepository.findById(accessUserId).orElseThrow(() -> new RingoException("Token not found", HttpStatus.BAD_REQUEST));
    if(accessUser.getRole().equals(Role.ADMIN)){
      return true;
    }
    return userId.equals(accessUserId);
  }

  @Async
  public void deleteImageInS3(String imageUrl){
    try {
      DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
          .bucket(bucket)
          .key(imageUrl)
          .build();
      amazonS3Client.deleteObject(deleteObjectRequest);
    }catch (Exception e){
      log.error("s3에 이미지를 삭제하는데 실패하였습니다. bucket: {}, image_url: {}", bucket, imageUrl, e);
      throw new RingoException("s3에 이미지를 삭제하는데 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private String getOriginalFilename(String ImageUrl){
    return  ImageUrl.substring(ImageUrl.lastIndexOf("/") + 1);
  }

  public void updateSnapImageDescription(UpdateSnapImageDescriptionRequestDto dto, Long snapImageId, Long userId){

    SnapImage image = snapImageRepository.findById(snapImageId)
        .orElseThrow(() -> new RingoException("스냅 이미지를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    if (!hasPermissionOnImage(image.getUser(), userId)){
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", userId, image.getUser().getId());
      throw new RingoException("스냅 이미지에 글을 수정할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }

    image.setDescription(dto.description());
    snapImageRepository.save(image);
  }

  public boolean isUnmoderateImage(MultipartFile file) {
    byte[] imageBytes = null;
    try {
      imageBytes = file.getBytes();
    }catch (Exception e){
      log.error("선정성 검사 중 이미지를 바이트로 변환하는데 실패하였습니다. filename: {}", file.getOriginalFilename(), e);
      throw new RingoException("선정성 검사 중 파일을 바이트로 변환하지 못하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
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
      log.error("이미지의 선정성 검사를 하는데 실패하였습니다. filename: {}", file.getOriginalFilename(), e);
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean verifyProfileImage(MultipartFile targetImage, User user){
    try {
      Profile profile = profileRepository.findByUser(user)
          .orElseThrow(() -> new RingoException("유저의 프로필이 존재하지 않습니다.", HttpStatus.BAD_REQUEST));
      String profileUrl = getOriginalFilename(profile.getImageUrl());
      GetObjectRequest getObjectRequest = GetObjectRequest.builder()
          .bucket(bucket)
          .key(profileUrl)
          .build();
      ResponseInputStream<GetObjectResponse> objectStream = amazonS3Client.getObject(getObjectRequest);

      return isSamePerson(objectStream.readAllBytes(), targetImage);
    }catch (Exception e){
      log.error("프로필 사진을 인증하는데 실패하였습니다. userId: {}", user.getId(), e);
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean isSamePerson(byte[] source, MultipartFile target){
    try{
      Image sourceImage = Image.builder().bytes(SdkBytes.fromByteArray(source)).build();
      Image targetImage = Image.builder().bytes(SdkBytes.fromByteArray(target.getBytes())).build();

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
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean existsFaceInImage(MultipartFile file){
    Image image;
    try {
      byte[] imageBytes = file.getBytes();
      image = Image.builder().bytes(SdkBytes.fromByteArray(imageBytes)).build();
    }catch (Exception e){
      log.error("이미지의 얼굴 여부 검수 중 파일을 바이트로 변환하는데 실패하였습니다. filename: {}", file.getOriginalFilename(), e);
      throw new RingoException("파일을 바이트로 변환하는데 실패하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    try {
      DetectFacesRequest request = DetectFacesRequest.builder().image(image).build();
      DetectFacesResponse result = amazonRekognition.detectFaces(request);
      List<FaceDetail> faceDetails = result.faceDetails();
      return !faceDetails.isEmpty();
    }catch (Exception e){
      log.error("이미지의 얼굴 존재여부를 검수하는데 실패하였습니다. filename: {}", file.getOriginalFilename(), e);
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
