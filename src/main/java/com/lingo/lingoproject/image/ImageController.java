package com.lingo.lingoproject.image;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.image.dto.GetFeedImageInfoResponseDto;
import com.lingo.lingoproject.image.dto.UploadAllFeedImageRequestDto;
import com.lingo.lingoproject.image.dto.GetImageUrlResponseDto;
import com.lingo.lingoproject.image.dto.UpdateFeedImageDescriptionRequestDto;
import com.lingo.lingoproject.user.UserService;
import com.lingo.lingoproject.utils.ApiListResponseDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
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
public class ImageController implements ImageApi{

  private final ImageService imageService;
  private final UserService userService;


  public ResponseEntity<?> uploadProfileImage(MultipartFile image, Long userId) {

    log.info("""

        userId={},
        step=프로필_업로드_시작,
        status=SUCCESS

        """, userId);
    GetImageUrlResponseDto dto = imageService.uploadProfileImage(image, userId);

    if(dto == null){
      log.info("""

          userId={},
          step=프로필_업로드_실패,
          status=FAILED,
          reason=이미_존재

          """, userId);
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto(
          ErrorCode.DUPLICATED.getCode(), "이미 프로필 사진이 존재합니다."));
    }
    log.info("""

        userId={},
        imageId={},
        step=프로필_업로드_완료,
        status=SUCCESS

        """, userId, dto.imageId());

    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }


  public ResponseEntity<GetImageUrlResponseDto> getProfileImageUrl(Long userId){

    log.info("""

        userId={},
        step=프로필_URL_조회_시작,
        status=SUCCESS

        """, userId);
    GetImageUrlResponseDto dto = imageService.getProfileImageUrl(userId);
    log.info("""

        userId={},
        step=프로필_URL_조회_완료,
        status=SUCCESS

        """, userId);

    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }


  public ResponseEntity<GetImageUrlResponseDto> updateProfileImage(
      MultipartFile image, Long profileId, User user
  ){
    log.info("""

        userId={},
        profileId={},
        step=프로필_업데이트_시작,
        status=SUCCESS

        """, user.getId(), profileId);
    GetImageUrlResponseDto dto = imageService.updateProfileImage(image, profileId, user.getId());
    log.info("""

        userId={},
        profileId={},
        step=프로필_업데이트_완료,
        status=SUCCESS

        """, user.getId(), profileId);

    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }


  public ResponseEntity<ResultMessageResponseDto> deleteProfileImage(Long profileId, User user){
    log.info("""

        userId={},
        profileId={},
        step=프로필_삭제_시작,
        status=SUCCESS

        """, user.getId(), profileId);
    imageService.deleteProfile(profileId, user.getId());
    log.info("""

        userId={},
        profileId={},
        step=프로필_삭제_완료,
        status=SUCCESS

        """, user.getId(), profileId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "이미지를 성공적으로 삭제했습니다."));
  }


  public ResponseEntity<ApiListResponseDto<GetImageUrlResponseDto>> uploadFeedImages(
      UploadAllFeedImageRequestDto images, Long userId
      //@AuthenticationPrincipal User user
  ){
//    if (!userId.equals(user.getId())){
//      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), userId);
//      throw new RingoException("피드사진을 업로드할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
//    }
    log.info("""

        userId={},
        step=피드_업로드_시작,
        status=SUCCESS

        """, userId);
    List<GetImageUrlResponseDto> dtos = imageService.uploadFeedImages(images.getList(), userId);
    log.info("""

        userId={},
        step=피드_업로드_완료,
        status=SUCCESS

        """, userId);

    return ResponseEntity.status(HttpStatus.CREATED).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), dtos));
  }

  public ResponseEntity<ApiListResponseDto<GetFeedImageInfoResponseDto>> getAllFeedImageUrls(Long userId){
    log.info("""

        userId={},
        step=피드_조회_시작,
        status=SUCCESS

        """, userId);
    List<GetFeedImageInfoResponseDto> responses = imageService.getAllFeedImageUrls(userId);
    log.info("""

        userId={},
        step=피드_조회_완료,
        status=SUCCESS

        """, userId);

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), responses));
  }


  public ResponseEntity<?> updateFeedImage(MultipartFile image, Long feedImageId, String description, User user
  ){
    log.info("""

        userId={},
        snapImageId={},
        step=피드_업데이트_시작,
        status=SUCCESS

        """, user.getId(), feedImageId);
    GetImageUrlResponseDto dto = imageService.updateFeedImage(image, feedImageId, description, user.getId());
    log.info("""

        userId={},
        snapImageId={},
        step=피드_업데이트_완료,
        status=SUCCESS

        """, user.getId(), feedImageId);

    if (dto == null){
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto(ErrorCode.UNMODERATE.getCode(), "부적절한 사진 입력"));
    }

    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }


  public ResponseEntity<ResultMessageResponseDto> deleteFeedImage(Long feedImageId, User user){
    log.info("""

        userId={},
        snapImageId={},
        step=피드_삭제_시작,
        status=SUCCESS

        """, user.getId(), feedImageId);
    imageService.deleteFeedImage(feedImageId, user.getId());
    log.info("""

        userId={},
        snapImageId={},
        step=피드_삭제_완료,
        status=SUCCESS

        """, user.getId(), feedImageId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "성공적으로 피드 사진을 삭제하였습니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> updateFeedImageDescription(Long feedImageId, UpdateFeedImageDescriptionRequestDto dto, User user){
    log.info("""

        userId={},
        snapImageId={},
        step=피드_설명_저장_시작,
        status=SUCCESS

        """, user.getId(), feedImageId);
    imageService.updateFeedImageDescription(dto, feedImageId, user.getId());
    log.info("""

        userId={},
        snapImageId={},
        step=피드_설명_저장_완료,
        status=SUCCESS

        """, user.getId(), feedImageId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "성공적으로 피드 사진 설명을 성공적으로 저장하였습니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> verifyProfile(MultipartFile image, User user){
    log.info("""

        userId={},
        step=프로필_얼굴인증_시작,
        status=SUCCESS

        """, user.getId());
    if (!imageService.verifyProfileImage(image, user)){
      log.info("""

          userId={},
          step=프로필_얼굴인증_미통과_완료,
          status=SUCCESS

          """, user.getId());
      return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResultMessageResponseDto(
          ErrorCode.INADEQUATE.getCode(), "얼굴이 인증되지 않았습니다."));
    }
    userService.updateUserProfileVerification(user);
    log.info("""

        userId={},
        step=프로필_얼굴인증_통과_완료,
        status=SUCCESS

        """, user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "얼굴이 성공적으로 인증되었습니다."));
  }
}
