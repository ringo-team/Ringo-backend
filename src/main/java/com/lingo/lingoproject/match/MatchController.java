package com.lingo.lingoproject.match;

import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.match.dto.GetMatchingRequestMessageResponseDto;
import com.lingo.lingoproject.match.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.match.dto.MatchingRequestDto;
import com.lingo.lingoproject.match.dto.RequestMatchingResponseDto;
import com.lingo.lingoproject.match.dto.SaveMatchingRequestMessageRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "matching-recommendation-controller", description = "매칭 관련 api")
public class MatchController {

  private final MatchService matchService;

  @Operation(
      summary = "매칭요청",
      description = "유저가 매칭 요청을 할 때 사용하는 api"
  )
  @ApiResponses(value = {
      @ApiResponse(
          responseCode = "0000",
          description = "요청 성공",
          content = @Content(schema = @Schema(implementation = RequestMatchingResponseDto.class))),
      @ApiResponse(
          responseCode = "E0003",
          description = "요청 권한이 없습니다.",
          content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(
          responseCode = "E0005",
          description = "해당 id로 유저를 찾을 수 없습니다.",
          content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(
          responseCode = "E0013",
          description = "매칭 점수가 기준 이하로 매칭되지 않았습니다.",
          content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(
          responseCode = "E1000",
          description = "내부 오류, 기타 문의",
          content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
      )
  })
  @PostMapping("/matches")
  public ResponseEntity<?> requestMatching(
      @Valid @RequestBody MatchingRequestDto matchingRequestDto,
      @AuthenticationPrincipal User user
  ) {
    try{
      // 매칭 요청자 검증
      Long requestUserId = matchingRequestDto.requestId();
      if (!requestUserId.equals(user.getId())){
        log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), requestUserId);
        throw new RingoException("매칭 요청자는 본인이어야 합니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
      }

      log.info("requestId={}, requestedId={}, step=매칭_요청_시작, status=SUCCESS",
          matchingRequestDto.requestId(),
          matchingRequestDto.requestedId());
      Matching matching = matchService.matchRequest(matchingRequestDto);
      log.info("requestId={}, requestedId={}, step=매칭_요청_완료, status=SUCCESS",
          matchingRequestDto.requestId(),
          matchingRequestDto.requestedId());

      if (matching == null){
        log.info("requestId={}, requestedId={}, step=매칭_점수_미충족, status=SUCCESS",
            matchingRequestDto.requestId(),
            matchingRequestDto.requestedId());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).body(
            new ResultMessageResponseDto(ErrorCode.INADEQUATE.getCode(), "매칭 점수가 기준 이하로 매칭되지 않았습니다."));
      }

      return ResponseEntity.status(HttpStatus.OK).body(new RequestMatchingResponseDto(ErrorCode.SUCCESS.getCode(), matching.getId()));
    }
    catch (Exception e){
      log.error("requestId={}, requestedId={}, step=매칭_요청_실패, status=SUCCESS",
          matchingRequestDto.requestId(),
          matchingRequestDto.requestedId(), e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("매칭 요청이 실패하였습니다:: " + e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(
      summary = "매칭 응답",
      description = "매칭 요청에 대한 응답(승낙, 거부)을 하는 api"
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "응답 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0004",
              description = "해당 id의 매칭 객체를 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "매칭을 수락할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PatchMapping("/matches/{matchingId}")
  public ResponseEntity<ResultMessageResponseDto>  responseToMatching(
      @NotNull @PathVariable(value = "matchingId") Long matchingId,
      @Parameter(
          description = "매칭 승낙 여부",
          example = "ACCEPTED",
          schema = @Schema(allowableValues = {"ACCEPTED", "REJECTED"})
      )
      @NotBlank @RequestParam(value = "decision") String decision,
      @AuthenticationPrincipal User user) {
    try {
      log.info("userId={}, matchingId={}, decision={}, step=매칭_응답_시작, status=SUCCESS", user.getId(), matchingId, decision);
      matchService.responseToRequest(decision, matchingId, user);
      log.info("userId={}, matchingId={}, decision={}, step=매칭_응답_완료, status=SUCCESS", user.getId(), matchingId, decision);

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "매칭 상태가 변경되었습니다."));

    } catch (Exception e) {
      log.error("userId={}, matchingId={}, decision={}, step=매칭_응답_실패, status=FAILED",
          user.getId(), matchingId, decision, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("매칭 응답에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "나에게/내가 매칭 요청한 사람 확인")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "조회 성공"
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "매칭 정보를 확인할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0007",
              description = "direction 값이 잘못 입력되었습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 운의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("users/{userId}/match-requests") // 나를 지목한 사람
  public ResponseEntity<JsonListWrapper<GetUserProfileResponseDto>> getMatchRequestsByDirection(
      @Parameter(description = "유저id", example = "5")
      @PathVariable(value = "userId") Long userId,

      @Parameter(
          description = "SENT 경우 내가 매칭 요청한 사람, RECEIVED 경우 나에게 매칭 요청한 사람",
          example = "SENT",
          schema = @Schema(allowableValues = {"SENT", "RECEIVED"})
      )
      @RequestParam(value = "direction") String direction,

      @AuthenticationPrincipal User user) {
    if (!userId.equals(user.getId())) {
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), userId);
      throw new RingoException("본인의 매칭 정보만 확인할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
    try {

      log.info("userId={}, direction={}, step=매칭_요청_확인_시작, status=SUCCESS", userId, direction);
      List<GetUserProfileResponseDto> responseDtoList = switch (direction) {
        case "SENT" -> matchService.getUserIdWhoRequestedByMe(user);
        case "RECEIVED" -> matchService.getUserIdWhoRequestToMe(user);
        default -> throw new RingoException("적절하지 않은 direction이 입력되었습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
      };
      log.info("userId={}, direction={}, step=매칭_요청_확인_완료, status=SUCCESS", userId, direction);

      return ResponseEntity.status(HttpStatus.OK)
          .body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), responseDtoList));

    }catch (Exception e){
      log.error("userId={}, direction={}, step=매칭_확인_실패, status=FAILED", user.getId(), direction);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("매칭 요청 정보를 확인하는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }


  @Operation(
      summary = "기본 이성 친구 추천",
      description = "매칭이 가능한 이성을 무작위로 골라 추천"
  )
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "추천 성공"
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "본인의 이성 추천만 확인할 수 있다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의"
          )
      }
  )
  @GetMapping("/users/{userId}/recommendations")
  public ResponseEntity<JsonListWrapper<GetUserProfileResponseDto>>  recommendByCumulativeSurveys(
      @Parameter(description = "유저id", example = "5")
      @PathVariable(value = "userId") Long userId,

      @AuthenticationPrincipal User user) {
    if (!userId.equals(user.getId())) {
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), userId);
      throw new RingoException("본인의 이성 추천만 확인할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }
    try {
      log.info("userId={}, step=이성_추천_시작, status=SUCCESS", userId);
      List<GetUserProfileResponseDto> rtnList = matchService.recommendByCumulativeSurvey(user);
      log.info("userId={}, step=이성_추천_시작, status=SUCCESS", userId);

      return ResponseEntity.status(HttpStatus.OK)
          .body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), rtnList));
    }catch (Exception e){
      log.error("userId={}, step=이성_추천_실패, status=FAILED", userId);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("이성 추천에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "설문을 통해 제공하는 이성추천", description = "일일 설문에 일치한 응답을 한 유저를 무작위로 추천")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "추천 성공"
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/users/{userId}/recommendations/daily-survey")
  public ResponseEntity<JsonListWrapper<GetUserProfileResponseDto>> recommendByDailySurvey(
      @PathVariable(value = "userId") Long userId,
      @AuthenticationPrincipal User user
  ){
    if (!userId.equals(user.getId())) {
      log.error("authUserId={}, userId={}, step=잘못된_유저_요청, status=FAILED", user.getId(), userId);
      throw new RingoException("본인의 이성 추천만 확인할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }
    try {
      log.info("userId={}, step=설문_기반_이성_추천_시작, status=SUCCESS", user.getId());
      List<GetUserProfileResponseDto> rtnList = matchService.recommendUserByDailySurvey(user);
      log.info("userId={}, step=설문_기반_이성_추천_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), rtnList));
    } catch (Exception e) {
      log.error("userId={}, step=설문_기반_이성_추천_실패, status=FAILED", user.getId());
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("설문 기반 이성추천에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "매칭 삭제")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "삭제 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "삭제할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0004",
              description = "해당 Id로 매칭 객체를 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @DeleteMapping("/matches/{matchingId}")
  public ResponseEntity<ResultMessageResponseDto> deleteMatching(
      @Parameter(description = "매칭 id", example = "5")
      @PathVariable("matchingId") Long matchingId,
      @AuthenticationPrincipal User user) {
    try {
      log.info("userId={}, matchingId={}, step=매칭_삭제_시작, status=SUCCESS", user.getId(), matchingId);
      matchService.deleteMatching(matchingId, user);
      log.info("userId={}, matchingId={}, step=매칭_삭제_완료, status=SUCCESS", user.getId(), matchingId);

      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "성공적으로 매칭을 삭제하였습니다."));
    } catch (Exception e) {
      log.error("userId={}, matchingId={}, step=매칭_삭제_실패, status=FAILED", user.getId(), matchingId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("매칭 삭제에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "매칭 요청 매세지 저장 및 수정")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "저장 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "메세지를 저장할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0004",
              description = "해당 Id로 매칭 객체를 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping("/matches/{matchingId}/message")
  public ResponseEntity<ResultMessageResponseDto> saveMatchingRequestMessage(
      @Valid @RequestBody SaveMatchingRequestMessageRequestDto dto,
      @PathVariable(value = "matchingId") Long matchingId,
      @AuthenticationPrincipal User user) {
    try {
      log.info("userId={}, matchingId={}, step=매칭_요청_메세지_저장_시작, status=SUCCESS", user.getId(), matchingId);
      matchService.saveMatchingRequestMessage(dto, matchingId, user);
      log.info("userId={}, matchingId={}, step=매칭_요청_메세지_저장_완료, status=SUCCESS", user.getId(), matchingId);

      return ResponseEntity.status(HttpStatus.OK)
          .body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "매칭 메세지가 성공적으로 저장되었습니다."));
    } catch (Exception e) {
      log.error("userId={}, matchingId={}, step=매칭_요청_메세지_저장_실패, status=FAILED", user.getId(), matchingId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("매칭 요청 메세지 저장에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "매칭 요청 메세지 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "조회 성공",
              content = @Content(schema = @Schema(implementation = GetMatchingRequestMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "메세지를 조회할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0004",
              description = "해당 Id로 매칭 객체를 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/matches/{matchingId}/message")
  public ResponseEntity<GetMatchingRequestMessageResponseDto> getMatchingRequestMessage(
      @PathVariable(value = "matchingId") Long matchingId,
      @AuthenticationPrincipal User user) {
    try {
      log.info("userId={}, matchingId={}, step=매칭_요청_메세지_조회_시작, status=SUCCESS", user.getId(),
          matchingId);
      String message = matchService.getMatchingRequestMessage(matchingId, user);
      log.info("userId={}, matchingId={}, step=매칭_요청_메세지_조회_완료, status=SUCCESS", user.getId(),
          matchingId);

      return ResponseEntity.status(HttpStatus.OK).body(new GetMatchingRequestMessageResponseDto(ErrorCode.SUCCESS.getCode(), message));
    } catch (Exception e) {
      log.error("userId={}, matchingId={}, step=매칭_요청_메세지_조화_실패, status=FAILED", user.getId(),
          matchingId, e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("매칭 요청 메세지 조회에 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "추천이성 가리기")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PatchMapping("users/{recommendedUserId}/recommendations/hide")
  public ResponseEntity<?> hideRecommendationUser(
      @Parameter(description = "가리고 싶은 유저의 아이디", example = "5")
      @PathVariable(value = "recommendedUserId") Long recommendedUserId,

      @AuthenticationPrincipal User user
  ){
    try{

      log.info("userId={}, recommendedUserId={}, step=추천이성_가리기_시작, status=SUCCESS", user.getId(), recommendedUserId);
      matchService.hideRecommendedUser(user, recommendedUserId);
      log.info("userId={}, recommendedUserId={}, step=추천이성_가리기_완료, status=SUCCESS", user.getId(), recommendedUserId);

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "추천이성 가리기 성공"));
    } catch (Exception e) {
      log.error("userId={}, recommendedUserId={}, step=추천이성_가리기_실패, status=FAILED", user.getId(), recommendedUserId, e);
      if (e instanceof RingoException re){
        throw re;
      }
      throw new RingoException("추천 이성을 가리는데 실패하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

}
