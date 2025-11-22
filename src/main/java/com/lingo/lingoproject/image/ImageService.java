package com.lingo.lingoproject.image;


import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.DetectModerationLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectModerationLabelsResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.ModerationLabel;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
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
import java.nio.ByteBuffer;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

  private final UserRepository userRepository;
  private final SnapImageRepository snapImageRepository;
  private final PhotographerImageRepository photographerImageRepository;
  private final AmazonS3Client amazonS3Client;
  private final ProfileRepository profileRepository;
  private final AmazonRekognition amazonRekognition;

  @Value("${aws.s3.bucket}")
  private String bucket;

  private final Float MIN_CONFIDENCE = 70f;


  /**
   * profile 이미지 crud
   */

  @Transactional
  public GetImageUrlResponseDto uploadProfileImage(MultipartFile file, Long userId) {

    if (!existsFaceInImage(file)){
      return null;
    }

    if (isUnmoderateImage(file)){
      return null;
    }

    User user = userRepository.findById(userId).orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));

    String imageUrl = uploadImageToS3(file);

    Profile profile = Profile.builder()
        .user(user)
        .imageUrl(imageUrl)
        .build();

    Profile savedProfile = profileRepository.save(profile);

    /*
     * 회원가입을 성공적으로 마무리 했으므로 user status를 COMPLETE로 변경한다.
     */
    user.setStatus(SignupStatus.COMPLETED);
    userRepository.save(user);

    return new GetImageUrlResponseDto(savedProfile.getImageUrl(), savedProfile.getId());
  }

  public GetImageUrlResponseDto getProfileImageUrl(Long userId){
    User user = userRepository.findById(userId).orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));
    Profile profile = profileRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("유저가 프로필을 가지지 않습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
    return new GetImageUrlResponseDto(profile.getImageUrl(), profile.getId());
  }

  @Transactional
  public GetImageUrlResponseDto updateProfileImage(MultipartFile file, Long profileId, Long userId){

    if (!existsFaceInImage(file)){
      return null;
    }

    if (isUnmoderateImage(file)){
      return null;
    }

    Profile profile = profileRepository.findById(profileId).orElseThrow(() -> new RingoException("Profile not found", HttpStatus.BAD_REQUEST));
    if(!hasPermissionOnImage(profile.getUser(), userId)){
      throw new RingoException("삭제할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
    String imageUrl = getOriginalFilename(profile.getImageUrl());
    // s3에 이미지 삭제
    deleteImageInS3(imageUrl);

    // s3에 새로운 이미지 파일 업로드
    imageUrl = uploadImageToS3(file);

    // profiles 테이블에 변경된 이미지 url 저장
    profile.setImageUrl(imageUrl);
    profileRepository.save(profile);
    return new GetImageUrlResponseDto(imageUrl, profileId);
  }

  @Transactional
  public List<GetImageUrlResponseDto> uploadSnapImages(List<MultipartFile> images, Long userId) {
    User user = userRepository.findById(userId).orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    List<SnapImage> snapImages = new ArrayList<>();

    for(MultipartFile file : images){

      if (isUnmoderateImage(file)){
        continue;
      }

      String imageUrl = uploadImageToS3(file);
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
    Profile profile = profileRepository.findById(profileId)
        .orElseThrow(() -> new RingoException("Profile not found", HttpStatus.BAD_REQUEST));
    if(!hasPermissionOnImage(profile.getUser(), userId)){
      return;
    }
    profileRepository.delete(profile);
    String imageUrl = getOriginalFilename(profile.getImageUrl());
    deleteImageInS3(imageUrl);
    log.info("{} 이미지가 삭제되었습니다.", imageUrl);
  }

  @Transactional
  public void deleteProfileImagesByUser(User user){
    List<Profile> profiles = profileRepository.findAllByUser(user);
    profileRepository.deleteAllByUser(user);

    for(Profile profile : profiles){
      deleteImageInS3(profile.getImageUrl());
    }
  }

  /**
   * snap 이미지 crud
   */

  public List<GetImageUrlResponseDto> getAllSnapImageUrls(Long userId){
    User user = userRepository.findById(userId).orElseThrow(() -> new RingoException("User not found", HttpStatus.BAD_REQUEST));
    List<SnapImage> images = snapImageRepository.findAllByUser(user);
    return images.stream()
        .map(image -> new GetImageUrlResponseDto(image.getImageUrl(), image.getId()))
        .toList();
  }

  @Transactional
  public GetImageUrlResponseDto updateSnapImage(MultipartFile file, Long snapImageId, String description, Long tokenUserId){

    if (isUnmoderateImage(file)){
      return null;
    }

    SnapImage snapImage = snapImageRepository.findById(snapImageId).orElseThrow(() -> new RingoException("Snap image not found", HttpStatus.BAD_REQUEST));
    if(!hasPermissionOnImage(snapImage.getUser(), tokenUserId)){
      throw new RingoException("업데이트 할 권한이 없습니다.", HttpStatus.FORBIDDEN);
    }
    String imageUrl = getOriginalFilename(snapImage.getImageUrl());

    //s3에 이미지 삭제
    deleteImageInS3(imageUrl);

    // s3에 새로운 이미지 업로드
    imageUrl = uploadImageToS3(file);

    // snap_images 테이블에 변경된 사진 url 저장
    snapImage.setImageUrl(imageUrl);
    snapImage.setDescription(description);
    snapImageRepository.save(snapImage);
    return new GetImageUrlResponseDto(imageUrl, snapImage.getId());
  }

  @Transactional
  public void deleteSnapImage(Long snapImageId, Long userId){
    SnapImage snapImage = snapImageRepository.findById(snapImageId)
        .orElseThrow(() -> new RingoException("Snap image not found", HttpStatus.BAD_REQUEST));
    if (!hasPermissionOnImage(snapImage.getUser(), userId)){
      return;
    }
    snapImageRepository.delete(snapImage);
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
      String imageUrl = uploadImageToS3(file);
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

  public  String uploadImageToS3(MultipartFile file){
    String originalFilename = file.getOriginalFilename();
    if(originalFilename != null && originalFilename.contains(".")){
      originalFilename = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    String filename = UUID.randomUUID().toString() + "_image_" + originalFilename;

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(file.getSize());
    metadata.setContentType(file.getContentType());

    String imageUrl = null;

    try {
      amazonS3Client.putObject(bucket, filename, file.getInputStream(), metadata);
      imageUrl = amazonS3Client.getUrl(bucket, filename).toString();
    }catch (Exception e){
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
      amazonS3Client.deleteObject(bucket, imageUrl);
    }catch (Exception e){
      throw new RingoException("s3에 이미지를 삭제하는데 문제가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  private String getOriginalFilename(String ImageUrl){
    return  ImageUrl.substring(ImageUrl.lastIndexOf("/") + 1);
  }

  public void updateSnapImageDescription(UpdateSnapImageDescriptionRequestDto dto, Long userId){

    SnapImage image = snapImageRepository.findById(dto.snapImageId())
        .orElseThrow(() -> new RingoException("스냅 이미지를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));

    if (!hasPermissionOnImage(image.getUser(), userId)){
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
      throw new RingoException("파일을 바이트로 변환하지 못하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    try {
      DetectModerationLabelsRequest request = new DetectModerationLabelsRequest()
          .withImage(new Image().withBytes(ByteBuffer.wrap(imageBytes)))
          .withMinConfidence(MIN_CONFIDENCE);

      DetectModerationLabelsResult result = amazonRekognition.detectModerationLabels(request);
      List<ModerationLabel> labels = result.getModerationLabels();

      return labels.stream().anyMatch(label -> {
        String name = label.getName();
        String parent = label.getParentName();

        if (label.getConfidence() < MIN_CONFIDENCE)
          return false;
        return
            (name != null && (name.contains("Nudity") || name.contains("Sexual") || name.contains("Suggestive")))
                || (parent != null && (parent.contains("Nudity") || parent.contains("Sexual") || parent.contains("Suggestive")));
      });
    }catch (Exception e){
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  public boolean existsFaceInImage(MultipartFile file){
    byte[] imageBytes = null;
    try {
      imageBytes = file.getBytes();
    }catch (Exception e){
      throw new RingoException("파일을 바이트로 변환하지 못하였습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    try {
      DetectFacesRequest request = new DetectFacesRequest()
          .withImage(new Image().withBytes(ByteBuffer.wrap(imageBytes)));
      DetectFacesResult result = amazonRekognition.detectFaces(request);
      List<FaceDetail> faceDetails = result.getFaceDetails();
      return !faceDetails.isEmpty();
    }catch (Exception e){
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
