package com.lingo.lingoproject.image;


import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.repository.ProfileRepository;
import com.lingo.lingoproject.repository.UserRepository;
import jakarta.transaction.Transactional;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageService {

  private final UserRepository userRepository;
  @Value("${aws.s3.bucket}")
  private String bucket;

  private final AmazonS3Client amazonS3Client;
  private final ProfileRepository profileRepository;

  @Transactional
  public String uploadImage(MultipartFile file, Long userId, int order) throws IOException {

    /**
     * 1. 유저 id로 해당 유저 찾기
     * 2. 파일명 생성하기
     * 3. s3에 profile 저장하기
     * 4. 데이터베이스에 profile imageUrl 저장하기
     */
    //1
    User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));

    //2
    String originalFilename = file.getOriginalFilename();
    if(originalFilename != null && originalFilename.contains(".")){
      originalFilename = originalFilename.substring(originalFilename.lastIndexOf("."));
    }
    String filename = UUID.randomUUID().toString() + "_profile" + originalFilename;

    ObjectMetadata metadata = new ObjectMetadata();
    metadata.setContentLength(file.getSize());
    metadata.setContentType(file.getContentType());

    //3
    String imageUrl = null;

    try {
      amazonS3Client.putObject(bucket, filename, file.getInputStream(), metadata);
      imageUrl = amazonS3Client.getUrl(bucket, filename).toString();
    }catch (Exception e){
      throw new IllegalArgumentException("s3에 이미지 업로드 하는데 실패하였습니다.");
    }

    //4
    Profile profile = Profile.builder()
        .user(user)
        .imageUrl(imageUrl)
        .order(order)
        .build();

    Profile savedProfile = profileRepository.save(profile);

    return savedProfile.getImageUrl();
  }

  public GetImageUrlRequestDto getImageUrl(Long userId, int order){
    User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    Profile profile = profileRepository.findByUserAndOrder(user, order);
    return new  GetImageUrlRequestDto(profile.getImageUrl(), order);
  }

  public List<GetImageUrlRequestDto> getAllImageUrls(Long userId){
    User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    List<Profile> profiles = profileRepository.findAllByUser(user);
    return profiles.stream()
        .map(profile -> new GetImageUrlRequestDto(profile.getImageUrl(), profile.getOrder()))
        .toList();
  }

  public void deleteProfile(Long userId, int order){
    User user = userRepository.findById(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    Profile profile = profileRepository.findByUserAndOrder(user, order);
    profileRepository.delete(profile);
    String imageUrl = getOriginalFilename(profile.getImageUrl());
    amazonS3Client.deleteObject(bucket, imageUrl);
    log.info("{} 이미지가 삭제되었습니다.", imageUrl);
  }

  private String getOriginalFilename(String ImageUrl){
    return  ImageUrl.substring(ImageUrl.lastIndexOf("/") + 1);
  }
}
