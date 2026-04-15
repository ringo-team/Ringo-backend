package com.lingo.lingoproject.notification.presentation;
import com.lingo.lingoproject.notification.application.FcmNotificationUseCase;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.notification.presentation.dto.GetNotificationResponseDto;
import com.lingo.lingoproject.notification.presentation.dto.SaveFcmTokenRequestDto;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "notification-controller", description = "Fcm 토큰 저장 api")
public class NotificationController {

  private final FcmNotificationUseCase fcmService;
  private final UserRepository userRepository;

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

    log.info("step=FCM_토큰_리프레시_시작, userId={}", user.getId());
    fcmService.refreshFcmToken(dto, user);
    log.info("step=FCM_토큰_리프레시_완료, userId={}", user.getId());

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "토큰이 정상적으로 저장되었습니다.")
        );
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
    log.info("step=알림_수신_설정_변경_시작, userId={}, notificationType={}", user.getId(), type);
    fcmService.alterNotificationOption(user, type);
    log.info("step=알림_수신_설정_변경_완료, userId={}, notificationType={}", user.getId(), type);

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "유저 알림 수신 여부를 성공적으로 변경하였습니다."));
  }

  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "성공"
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "조회 권한이 없는 유저",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/notifications")
  public ResponseEntity<ApiListResponseDto<GetNotificationResponseDto>> getUserNotification(
      @Parameter(name = "유저 id", description = "알림을 조회할 유저의 id")
      @RequestParam(value = "userId") Long userId,

      @AuthenticationPrincipal User user
  ){
    if (!userId.equals(user.getId())){
      throw new RingoException("조회 권한이 없는 유저입니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    log.info("step=알림_조회_시작, userId={}", user.getId());
    List<GetNotificationResponseDto> dto = fcmService.getNotificationMessage(user);
    log.info("step=알림_조회_완료, userId={}", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), dto));
  }
}
