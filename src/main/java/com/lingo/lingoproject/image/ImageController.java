package com.lingo.lingoproject.image;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.image.dto.FeedImageDataListDto;
import com.lingo.lingoproject.image.dto.GetImageUrlResponseDto;
import com.lingo.lingoproject.image.dto.UpdateFeedImageDescriptionRequestDto;
import com.lingo.lingoproject.user.UserService;
import com.lingo.lingoproject.utils.JsonListWrapper;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping()
@Slf4j
@RequiredArgsConstructor
public class ImageController {

  private final ImageService imageService;
  private final UserService userService;

  @Operation(
      summary = "프로필 이미지 업로드",
      description = "s3에 이미지를 업로드합니다"
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "생성 성공",
              content = @Content(schema = @Schema(implementation = GetImageUrlResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0008",
              description = "이미 프로필이 업로드 되어 있음",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0005",
              description = "유저를 찾을 수 없음",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0010",
              description = "프로필에 얼굴이 나오지 않음",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0011",
              description = "프로필에 부적절한 부분이 검출됨",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부에러, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping(value = "profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> uploadProfileImage(
      @Parameter(description = "이미지 파일")
      @RequestParam(value = "image") MultipartFile image,

      @NotNull Long userId
      ) {

    try {

      log.info("userId={}, step=프로필_업로드_시작, status=SUCCESS", userId);
      GetImageUrlResponseDto dto = imageService.uploadProfileImage(image, userId);

      if(dto == null){
        log.info("userId={}, step=프로필_업로드_실패, status=FAILED, reason=이미_존재", userId);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto(
            ErrorCode.DUPLICATED.getCode(), "이미 프로필 사진이 존재합니다."));
      }
      log.info("userId={}, imageId={}, step=프로필_업로드_완료, status=SUCCESS", userId, dto.imageId());

      return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }catch (Exception e){
      log.error("userId={}, step=프로필_업로드_실패, status=FAILED", userId, e);
      if(e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("프로필 업로드에 실패하였습니다", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "프로필 조회",
      description = "프로필 URL과 이미지 id를 반환합니다."
  )
  @ApiResponses(
      value = {
          @ApiResponse(
            responseCode = "0000",
            description = "조회 성공",
            content = @Content(schema = @Schema(implementation = GetImageUrlResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0005",
              description = "해당 id로 유저를 찾을 수 없음",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 에러, 기타문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/users/{userId}/profile")
  public ResponseEntity<GetImageUrlResponseDto> getProfileImageUrl(
      @Parameter(description = "이미지 id", example = "5")
      @PathVariable(value = "userId") Long userId){

    try {
      log.info("userId={}, step=프로필_URL_조회_시작, status=SUCCESS", userId);
      GetImageUrlResponseDto dto = imageService.getProfileImageUrl(userId);
      log.info("userId={}, step=프로필_URL_조회_완료, status=SUCCESS", userId);

      return ResponseEntity.status(HttpStatus.OK).body(dto);
    }catch (Exception e){
      log.error("userId={}, step=프로필_URL_조회_실패, status=FAILED", userId, e);
      if(e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("프로필 조회에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "프로필 이미지 업데이트")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "업데이트 성공",
              content = @Content(schema = @Schema(implementation = GetImageUrlResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "업데이트할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0010",
              description = "프로필에 얼굴이 나오지 않음",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0011",
              description = "프로필에 부적절한 부분이 검출됨",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
      }
  )
  @PatchMapping(value = "profiles/{profileId}", consumes =  MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<GetImageUrlResponseDto> updateProfileImage(
      @Parameter(description = "업데이트할 사진")
      @RequestParam(value = "image") MultipartFile image,

      @Parameter(description = "업데이트 할 프로필 사진의 id")
      @PathVariable(value = "profileId") Long profileId,

      @AuthenticationPrincipal User user
  ){
    try {

      log.info("userId={}, profileId={}, step=프로필_업데이트_시작, status=SUCCESS", user.getId(), profileId);
      GetImageUrlResponseDto dto = imageService.updateProfileImage(image, profileId, user.getId());
      log.info("userId={}, profileId={}, step=프로필_업데이트_완료, status=SUCCESS", user.getId(), profileId);

      return ResponseEntity.status(HttpStatus.OK).body(dto);

    }catch (Exception e){
      log.error("userId={}, profileId={}, step=프로필_업데이트_실패, status=FAILED", user.getId(), profileId, e);
      if(e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("프로필 업데이트에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "프로필 사진 삭제",
      description = "프로필 id에 해당하는 이미지를 삭제합니다."
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "삭제 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "삭제 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부에러, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @DeleteMapping("/profiles/{profileId}")
  public ResponseEntity<ResultMessageResponseDto> deleteProfileImage(
      @Parameter(description = "프로필 id", example = "12")
      @PathVariable(value = "profileId") Long profileId,

      @AuthenticationPrincipal User user
  ){
    try {

      log.info("userId={}, profileId={}, step=프로필_삭제_시작, status=SUCCESS", user.getId(), profileId);
      imageService.deleteProfile(profileId, user.getId());
      log.info("userId={}, profileId={}, step=프로필_삭제_완료, status=SUCCESS", user.getId(), profileId);

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "이미지를 성공적으로 삭제했습니다."));

    }catch (Exception e){
      log.error("userId={}, profileId={}, step=프로필_삭제_실패, status=FAILED", user.getId(), profileId);
      if(e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("프로필 삭제에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "피드 사진 일괄 업로드")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "업로드 성공",
              content = @Content(schema = @Schema(implementation = JsonListWrapper.class))
          ),
          @ApiResponse(
              responseCode = "E0007",
              description = "잘못된 유저 id 파라미터",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0012",
              description = "업로드 사진 개수 초과",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping(value = "/feeds", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<JsonListWrapper<GetImageUrlResponseDto>> uploadFeedImages(
      @Parameter(description = "이미지 파일들 업로드")
      @ModelAttribute FeedImageDataListDto images,

      @Parameter(description = "유저 id", example = "5")
      @RequestParam("userId") Long userId

      //@AuthenticationPrincipal User user
  ){
//    if (!userId.equals(user.getId())){
//      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), userId);
//      throw new RingoException("피드사진을 업로드할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
//    }
    try {

      log.info("userId={}, step=피드_업로드_시작, status=SUCCESS", userId);
      List<GetImageUrlResponseDto> dtos = imageService.uploadFeedImages(images.getList(), userId);
      log.info("userId={}, step=피드_업로드_완료, status=SUCCESS", userId);

      return ResponseEntity.status(HttpStatus.CREATED)
          .body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), dtos));

    } catch (Exception e) {
      log.error("userId={}, step=피드_업로드_실패, status=FAILED", userId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("피드 이미지를 업로드하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "피드 이미지 조회.",
      description = "유저가 올린 모든 피드 사진을 조회"
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "조회 성공",
              content = @Content(schema = @Schema(implementation = JsonListWrapper.class))
          ),
          @ApiResponse(
              responseCode = "E0005",
              description = "id로 유저를 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("users/{userId}/feeds")
  public ResponseEntity<JsonListWrapper<GetImageUrlResponseDto>> getAllFeedImageUrls(
      @Parameter(description = "유저 Id", example = "5")
      @PathVariable(value = "userId") Long userId
  ){
    try {
      log.info("userId={}, step=피드_조회_시작, status=SUCCESS", userId);
      List<GetImageUrlResponseDto> responses = imageService.getAllFeedImageUrls(userId);
      log.info("userId={}, step=피드_조회_완료, status=SUCCESS", userId);

      return ResponseEntity.status(HttpStatus.OK)
          .body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), responses));

    } catch (Exception e) {
      log.error("userId={}, step=피드_조회_실패, status=FAILED", userId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("피드 이미지를 조회하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "피드 사진 업데이트")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "업데이트 성공",
              content = @Content(schema = @Schema(implementation = GetImageUrlResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0011",
              description = "부적절한 사진 사용",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0004",
              description = "해당 id로 피드사진을 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "피드사진을 업데이트할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PatchMapping("/feeds/{feedImageId}")
  public ResponseEntity<?> updateFeedImage(
      @Parameter(description = "업데이트할 사진")
      @RequestParam(value = "image") MultipartFile image,

      @Parameter(description = "업데이트할 피드 사진의 id")
      @PathVariable(value = "feedImageId") Long feedImageId,

      @Parameter(description = "업데이트한 피드 사진의 설명")
      @RequestParam(value = "description") String description,

      @AuthenticationPrincipal User user
  ){
    try {

      log.info("userId={}, snapImageId={}, step=피드_업데이트_시작, status=SUCCESS", user.getId(), feedImageId);
      GetImageUrlResponseDto dto = imageService.updateFeedImage(image, feedImageId, description, user.getId());
      log.info("userId={}, snapImageId={}, step=피드_업데이트_완료, status=SUCCESS", user.getId(), feedImageId);

      if (dto == null){
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto(
            ErrorCode.UNMODERATE.getCode(), "부적절한 사진 입력"));
      }

      return ResponseEntity.status(HttpStatus.OK).body(dto);

    } catch (Exception e) {
      log.error("userId={}, snapImageId={}, step=피드_업데이트_실패, status=FAILED", user.getId(), feedImageId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("피드 이미지를 수정하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "피드 사진 삭제",
      description = "피드 사진 id에 해당하는 이미지를 삭제합니다."
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "삭제 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0004",
              description = "id에 해당하는 피드사진을 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "삭제 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부에러, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @DeleteMapping("/feeds/{feedImageId}")
  public ResponseEntity<ResultMessageResponseDto> deleteFeedImage(
      @Parameter(description = "피드 사진 id", example = "11")
      @PathVariable(value = "feedImageId") Long feedImageId,

      @AuthenticationPrincipal User user
  ){
    try {

      log.info("userId={}, snapImageId={}, step=피드_삭제_시작, status=SUCCESS", user.getId(), feedImageId);
      imageService.deleteFeedImage(feedImageId, user.getId());
      log.info("userId={}, snapImageId={}, step=피드_삭제_완료, status=SUCCESS", user.getId(), feedImageId);

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "성공적으로 피드 사진을 삭제하였습니다."));

    } catch (Exception e) {
      log.error("userId={}, snapImageId={}, step=피드_삭제_실패, status=FAILED", user.getId(), feedImageId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("피드 이미지를 삭제하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "피드 사진 설명 저장")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "저장 완료",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0004",
              description = "해당 id로 피드사진을 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "피드사진을 저장할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PatchMapping("/feeds/{feedImageId}/description")
  public ResponseEntity<ResultMessageResponseDto> updateFeedImageDescription(
      @Parameter(description = "피드 사진 id", example = "11")
      @PathVariable(value = "feedImageId") Long feedImageId,

      @RequestBody UpdateFeedImageDescriptionRequestDto dto,

      @AuthenticationPrincipal User user
  ){
    try {

      log.info("userId={}, snapImageId={}, step=피드_설명_저장_시작, status=SUCCESS", user.getId(), feedImageId);
      imageService.updateFeedImageDescription(dto, feedImageId, user.getId());
      log.info("userId={}, snapImageId={}, step=피드_설명_저장_완료, status=SUCCESS", user.getId(), feedImageId);

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "성공적으로 피드 사진 설명을 성공적으로 저장하였습니다."));

    } catch (Exception e) {
      log.error("userId={}, snapImageId={}, step=피드_설명_저장_실패, status=FAILED", user.getId(), feedImageId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("피드 사진 설명을 저장하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "얼굴 인증")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "얼굴 인증 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0013",
              description = "얼굴 인증 실패",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping(value = "/profiles/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ResultMessageResponseDto> verifyProfile(
      @RequestParam(value = "image") MultipartFile image,
      @AuthenticationPrincipal User user
  ){
    try {

      log.info("userId={}, step=프로필_얼굴인증_시작, status=SUCCESS", user.getId());
      if (!imageService.verifyProfileImage(image, user)){
        log.info("userId={}, step=프로필_얼굴인증_미통과_완료, status=SUCCESS", user.getId());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(new ResultMessageResponseDto(
            ErrorCode.INADEQUATE.getCode(), "얼굴이 인증되지 않았습니다."));
      }
      userService.updateUserProfileVerification(user);
      log.info("userId={}, step=프로필_얼굴인증_통과_완료, status=SUCCESS", user.getId());
      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "얼굴이 성공적으로 인증되었습니다."));

    } catch (Exception e) {
      log.error("userId={}, step=프로필_얼굴인증_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("프로필 얼굴 인증에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
