package com.lingo.lingoproject.image;
import com.lingo.lingoproject.image.application.S3ImageStorageService;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.image.dto.GetFeedImageInfoResponseDto;
import com.lingo.lingoproject.image.dto.UploadAllFeedImageRequestDto;
import com.lingo.lingoproject.image.dto.GetImageUrlResponseDto;
import com.lingo.lingoproject.image.dto.UpdateFeedImageDescriptionRequestDto;
import com.lingo.lingoproject.user.application.UserUpdateUseCase;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping
@Slf4j
@RequiredArgsConstructor
public class ImageController implements ImageApi {

  private final S3ImageStorageService imageService;
  private final UserUpdateUseCase userUpdateUseCase;


  public ResponseEntity<?> uploadProfileImage(MultipartFile image, User user) {
    log.info("step=프로필_업로드_시작, userId={}", user.getId());
    GetImageUrlResponseDto dto = imageService.uploadProfileImage(image, user);

    if (dto == null) {
      log.info("step=프로필_업로드_실패, userId={}, reason=이미_존재", user.getId());
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto(
          ErrorCode.PROFILE_DUPLICATED.getCode(), "이미 프로필 사진이 존재합니다."));
    }

    log.info("step=프로필_업로드_완료, userId={}, imageId={}", user.getId(), dto.imageId());
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }


  public ResponseEntity<GetImageUrlResponseDto> getProfileImageUrl(Long userId) {
    log.info("step=프로필_URL_조회_시작, userId={}", userId);
    GetImageUrlResponseDto dto = imageService.fetchProfileImageUrl(userId);
    log.info("step=프로필_URL_조회_완료, userId={}", userId);
    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }


  public ResponseEntity<GetImageUrlResponseDto> updateProfileImage(MultipartFile image, Long profileId, User user) {
    log.info("step=프로필_업데이트_시작, userId={}, profileId={}", user.getId(), profileId);
    GetImageUrlResponseDto dto = imageService.updateProfileImage(image, profileId, user.getId());
    log.info("step=프로필_업데이트_완료, userId={}, profileId={}", user.getId(), profileId);
    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }


  public ResponseEntity<ResultMessageResponseDto> deleteProfileImage(Long profileId, User user) {
    log.info("step=프로필_삭제_시작, userId={}, profileId={}", user.getId(), profileId);
    imageService.deleteProfileImage(user);
    log.info("step=프로필_삭제_완료, userId={}, profileId={}", user.getId(), profileId);
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "이미지를 성공적으로 삭제했습니다."));
  }


  public ResponseEntity<ApiListResponseDto<GetImageUrlResponseDto>> uploadFeedImages(
      UploadAllFeedImageRequestDto images, User user) {
    log.info("step=피드_업로드_시작, userId={}", user.getId());
    List<GetImageUrlResponseDto> dtos = imageService.uploadFeedImages(images.getList(), user);
    log.info("step=피드_업로드_완료, userId={}, count={}", user.getId(), dtos.size());
    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), dtos));
  }

  public ResponseEntity<ApiListResponseDto<GetFeedImageInfoResponseDto>> getAllFeedImageUrls(Long userId) {
    log.info("step=피드_조회_시작, userId={}", userId);
    List<GetFeedImageInfoResponseDto> responses = imageService.fetchFeedImages(userId);
    log.info("step=피드_조회_완료, userId={}", userId);
    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), responses));
  }


  public ResponseEntity<?> updateFeedImage(MultipartFile image, Long feedImageId, String description, User user) {
    log.info("step=피드_업데이트_시작, userId={}, feedImageId={}", user.getId(), feedImageId);
    GetImageUrlResponseDto dto = imageService.updateFeedImage(image, feedImageId, description, user.getId());
    log.info("step=피드_업데이트_완료, userId={}, feedImageId={}", user.getId(), feedImageId);

    if (dto == null) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto(ErrorCode.UNMODERATE.getCode(), "부적절한 사진 입력"));
    }
    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }


  public ResponseEntity<ResultMessageResponseDto> deleteFeedImage(Long feedImageId, User user) {
    log.info("step=피드_삭제_시작, userId={}, feedImageId={}", user.getId(), feedImageId);
    imageService.deleteFeedImage(feedImageId, user.getId());
    log.info("step=피드_삭제_완료, userId={}, feedImageId={}", user.getId(), feedImageId);
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "성공적으로 피드 사진을 삭제하였습니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> updateFeedImageDescription(Long feedImageId, UpdateFeedImageDescriptionRequestDto dto, User user) {
    log.info("step=피드_설명_저장_시작, userId={}, feedImageId={}", user.getId(), feedImageId);
    imageService.updateFeedImageDescription(dto, feedImageId, user.getId());
    log.info("step=피드_설명_저장_완료, userId={}, feedImageId={}", user.getId(), feedImageId);
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "성공적으로 피드 사진 설명을 성공적으로 저장하였습니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> verifyProfile(MultipartFile image, User user) {
    log.info("step=프로필_얼굴인증_시작, userId={}", user.getId());
    if (!imageService.verifyFaceIdentity(image, user)) {
      log.info("step=프로필_얼굴인증_미통과, userId={}", user.getId());
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResultMessageResponseDto(
          ErrorCode.INADEQUATE.getCode(), "얼굴이 인증되지 않았습니다."));
    }
    userUpdateUseCase.updateUserProfileVerification(user);
    log.info("step=프로필_얼굴인증_통과, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "얼굴이 성공적으로 인증되었습니다."));
  }
}
