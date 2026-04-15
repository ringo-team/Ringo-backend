package com.lingo.lingoproject.matching.presentation;
import com.lingo.lingoproject.matching.application.MatchService;
import com.lingo.lingoproject.shared.domain.model.Matching;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.matching.presentation.dto.GetMatchingRequestMessageResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.GetScrappedUserResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.MatchingRequestDto;
import com.lingo.lingoproject.matching.presentation.dto.RequestMatchingResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.SaveMatchingRequestMessageRequestDto;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
public class MatchController implements MatchingApi {

  private final MatchService matchService;


  public ResponseEntity<?> requestMatching(MatchingRequestDto matchingRequestDto, @AuthenticationPrincipal User user) {
    // 매칭 요청자 검증
    Long requestUserId = matchingRequestDto.requestId();
    if (!requestUserId.equals(user.getId())) {
      log.error("step=잘못된_유저_요청, authUserId={}, userId={}, status=FAILED", user.getId(), requestUserId);
      throw new RingoException("매칭 요청자는 본인이어야 합니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

    log.info("step=매칭_요청_시작, requestId={}, requestedId={}", matchingRequestDto.requestId(), matchingRequestDto.requestedId());
    Matching matching = matchService.matchRequest(matchingRequestDto);
    log.info("step=매칭_요청_완료, requestId={}, requestedId={}", matchingRequestDto.requestId(), matchingRequestDto.requestedId());

    return ResponseEntity.status(HttpStatus.OK).body(new RequestMatchingResponseDto(ErrorCode.SUCCESS.getCode(), matching.getId()));
  }


  public ResponseEntity<ResultMessageResponseDto> responseToMatching(Long matchingId, String decision, User user) {
    log.info("step=매칭_응답_시작, userId={}, matchingId={}, decision={}", user.getId(), matchingId, decision);
    matchService.respondToMatchingRequest(decision, matchingId, user);
    log.info("step=매칭_응답_완료, userId={}, matchingId={}, decision={}", user.getId(), matchingId, decision);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "매칭 상태가 변경되었습니다."));
  }


  public ResponseEntity<ApiListResponseDto<GetUserProfileResponseDto>> getMatchRequestsByDirection(Long userId, String direction, User user) {
    if (!userId.equals(user.getId())) {
      log.error("step=잘못된_유저_요청, authUserId={}, userId={}, status=FAILED", user.getId(), userId);
      throw new RingoException("본인의 매칭 정보만 확인할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
    log.info("step=매칭_요청_확인_시작, userId={}, direction={}", userId, direction);
    List<GetUserProfileResponseDto> responseDtoList = switch (direction) {
      case "SENT" -> matchService.getSentMatchingProfiles(user);
      case "RECEIVED" -> matchService.getReceivedMatchingProfiles(user);
      default -> throw new RingoException("적절하지 않은 direction이 입력되었습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
    };
    log.info("step=매칭_요청_확인_완료, userId={}, direction={}", userId, direction);

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), responseDtoList));
  }


  public ResponseEntity<ApiListResponseDto<GetUserProfileResponseDto>> recommendByCumulativeSurveys(Long userId, User user) {
    if (!userId.equals(user.getId())) {
      log.error("step=잘못된_유저_요청, authUserId={}, userId={}, status=FAILED", user.getId(), userId);
      throw new RingoException("본인의 이성 추천만 확인할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }
    log.info("step=이성_추천_시작, userId={}", userId);
    List<GetUserProfileResponseDto> rtnList = matchService.getRecommendationsByCumulativeSurvey(user);
    log.info("step=이성_추천_완료, userId={}", userId);

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), rtnList));
  }

  public ResponseEntity<ApiListResponseDto<GetUserProfileResponseDto>> recommendByDailySurvey(Long userId, User user) {
    if (!userId.equals(user.getId())) {
      log.error("step=잘못된_유저_요청, authUserId={}, userId={}, status=FAILED", user.getId(), userId);
      throw new RingoException("본인의 이성 추천만 확인할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }
    log.info("step=설문_기반_이성_추천_시작, userId={}", user.getId());
    List<GetUserProfileResponseDto> rtnList = matchService.getRecommendationsByDailySurvey(user);
    log.info("step=설문_기반_이성_추천_완료, userId={}", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), rtnList));
  }


  public ResponseEntity<ResultMessageResponseDto> deleteMatching(Long matchingId, User user) {
    log.info("step=매칭_삭제_시작, userId={}, matchingId={}", user.getId(), matchingId);
    matchService.deleteMatching(matchingId, user);
    log.info("step=매칭_삭제_완료, userId={}, matchingId={}", user.getId(), matchingId);

    return ResponseEntity.status(HttpStatus.OK)
        .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "성공적으로 매칭을 삭제하였습니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> saveMatchingRequestMessage(SaveMatchingRequestMessageRequestDto dto, Long matchingId, User user) {
    log.info("step=매칭_요청_메세지_저장_시작, userId={}, matchingId={}", user.getId(), matchingId);
    matchService.saveMatchingRequestMessage(dto, matchingId, user);
    log.info("step=매칭_요청_메세지_저장_완료, userId={}, matchingId={}", user.getId(), matchingId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "매칭 메세지가 성공적으로 저장되었습니다."));
  }


  public ResponseEntity<GetMatchingRequestMessageResponseDto> getMatchingRequestMessage(Long matchingId, User user) {
    log.info("step=매칭_요청_메세지_조회_시작, userId={}, matchingId={}", user.getId(), matchingId);
    String message = matchService.getMatchingRequestMessage(matchingId, user);
    log.info("step=매칭_요청_메세지_조회_완료, userId={}, matchingId={}", user.getId(), matchingId);

    return ResponseEntity.status(HttpStatus.OK).body(new GetMatchingRequestMessageResponseDto(ErrorCode.SUCCESS.getCode(), message));
  }


  public ResponseEntity<?> hideRecommendationUser(Long recommendedUserId, User user) {
    log.info("step=추천이성_가리기_시작, userId={}, recommendedUserId={}", user.getId(), recommendedUserId);
    matchService.hideRecommendedUser(user, recommendedUserId);
    log.info("step=추천이성_가리기_완료, userId={}, recommendedUserId={}", user.getId(), recommendedUserId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "추천이성 가리기 성공"));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> scrapUser(Long recommendedUserId, User user) {
    log.info("step=추천이성_저장_시작, userId={}, recommendedUserId={}", user.getId(), recommendedUserId);
    matchService.scrapUser(recommendedUserId, user);
    log.info("step=추천이성_저장_완료, userId={}, recommendedUserId={}", user.getId(), recommendedUserId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "추천이성 스크랩에 성공하였습니다."));
  }

  @Override
  public ResponseEntity<ApiListResponseDto<GetScrappedUserResponseDto>> getScrappedUser(Long userId, User user) {
    if (!userId.equals(user.getId())) {
      throw new RingoException("자신이 스크랩한 유저만 확인할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }
    log.info("step=스크랩된_유저_조회_시작, userId={}", user.getId());
    List<GetScrappedUserResponseDto> result = matchService.getScrappedUsers(user);
    log.info("step=스크랩된_유저_조회_완료, userId={}", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result));
  }

}
