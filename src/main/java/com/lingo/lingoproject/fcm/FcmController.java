package com.lingo.lingoproject.fcm;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.fcm.dto.SaveFcmTokenRequestDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "notification-controller", description = "Fcm 토큰 저장 api")
public class FcmController {

  private final FcmService fcmService;

  @PostMapping("/fcm/refresh")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "리프레시 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "토큰을 리프레시할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  public ResponseEntity<ResultMessageResponseDto> refreshFcmToken(
      @RequestBody SaveFcmTokenRequestDto dto,
      @AuthenticationPrincipal User user
  ){
    if(!dto.userId().equals(user.getId())){
      throw new RingoException("토큰을 refresh할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    try {

      log.info("userId={}, step=리프레시_토큰_시작, status=SUCCESS", user.getId());
      fcmService.refreshFcmToken(dto, user);
      log.info("userId={}, step=리프레시_토큰_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "토큰이 정상적으로 저장되었습니다.")
      );

    }catch (Exception e){
      log.error("userId={}, step=리프레시_토큰_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("토큰을 리프레시하는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0007",
              description = "잘못된 파라미터 전달",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping("/notification/out")
  public ResponseEntity<ResultMessageResponseDto> alterNotificationOptionReception(

      @Parameter(description = "notification 타입", example = "MARKETING")
      @RequestParam String type,

      @AuthenticationPrincipal User user
  ){
    try {
      log.info("userId={}, notificationType={}, step=유저알림_수신_여부_변경_시작, status=SUCCESS", user.getId(), type);
      fcmService.alterNotificationOption(user, type);
      log.info("userId={}, notificationType={}, step=유저알림_수신_여부_변경_완료, status=SUCCESS", user.getId(), type);

      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저 알림 수신 여부를 성공적으로 변경하였습니다."));
    }catch (Exception e){
      log.error("userId={}, notificationType={}, step=알림수신_여부_변경_실패, status=FAILED", user.getId(), type, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("알림수신 여부를 변경하는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
