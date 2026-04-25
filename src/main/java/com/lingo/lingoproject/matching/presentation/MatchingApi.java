package com.lingo.lingoproject.matching.presentation;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.matching.presentation.dto.GetMatchingRequestMessageResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.GetScrappedUserResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.MatchingRequestDto;
import com.lingo.lingoproject.matching.presentation.dto.RequestMatchingResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.SaveMatchingRequestMessageRequestDto;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "matching-recommendation-controller", description = "매칭 관련 api")
public interface MatchingApi {
  @Operation(summary = "매칭요청", description = "유저가 매칭 요청을 할 때 사용하는 api")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "요청 성공", content = @Content(schema = @Schema(implementation = RequestMatchingResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "요청 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0005", description = "해당 id로 유저를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/matches")
  ResponseEntity<RequestMatchingResponseDto> requestMatching(
      @Valid @RequestBody MatchingRequestDto matchingRequestDto,
      @AuthenticationPrincipal User user
  );

  @Operation(summary = "매칭 응답", description = "매칭 요청에 대한 응답(승낙, 거부)을 하는 api")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "응답 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0004", description = "해당 id의 매칭 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "매칭을 수락할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
      }
  )
  @PatchMapping("/matches/{matching-id}")
  ResponseEntity<ResultMessageResponseDto>  responseToMatching(
      @NotNull @PathVariable(value = "matching-id") Long matchingId,
      @Parameter(description = "매칭 승낙 여부", example = "ACCEPTED", schema = @Schema(allowableValues = {"ACCEPTED", "REJECTED"})) @NotBlank @RequestParam(value = "decision") String decision,
      @AuthenticationPrincipal User user);

  @Operation(summary = "나에게/내가 매칭 요청한 사람 확인")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "조회 성공"),
          @ApiResponse(responseCode = "E0003", description = "매칭 정보를 확인할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0007", description = "direction 값이 잘못 입력되었습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 운의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
      }
  )
  @GetMapping("users/{user-id}/match-requests")
  ResponseEntity<ApiListResponseDto<GetUserProfileResponseDto>> getMatchRequestsByDirection(
      @Parameter(description = "유저id", example = "5") @PathVariable(value = "user-id") Long userId,
      @Parameter(description = "SENT 경우 내가 매칭 요청한 사람, RECEIVED 경우 나에게 매칭 요청한 사람", example = "SENT", schema = @Schema(allowableValues = {"SENT", "RECEIVED"})) @RequestParam(value = "direction") String direction,
      @AuthenticationPrincipal User user);


  @Operation(summary = "기본 이성 친구 추천", description = "매칭이 가능한 이성을 무작위로 골라 추천")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "추천 성공"),
          @ApiResponse(responseCode = "E0003", description = "본인의 이성 추천만 확인할 수 있다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의")
  })
  @GetMapping("/users/{user-id}/recommendations")
  ResponseEntity<ApiListResponseDto<GetUserProfileResponseDto>>  recommendByCumulativeSurveys(
      @Parameter(description = "유저id", example = "5") @PathVariable(value = "user-id") Long userId,
      @AuthenticationPrincipal User user);

  @Operation(summary = "설문을 통해 제공하는 이성추천", description = "일일 설문에 일치한 응답을 한 유저를 무작위로 추천")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "추천 성공"),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/users/{user-id}/recommendations/daily-survey")
  ResponseEntity<ApiListResponseDto<GetUserProfileResponseDto>> recommendByDailySurvey(
      @PathVariable(value = "user-id") Long userId, @AuthenticationPrincipal User user);

  @Operation(summary = "매칭 삭제")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "삭제 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "삭제할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0004", description = "해당 Id로 매칭 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
      }
  )
  @DeleteMapping("/matches/{matching-id}")
  ResponseEntity<ResultMessageResponseDto> deleteMatching(
      @Parameter(description = "매칭 id", example = "5") @PathVariable("matching-id") Long matchingId,
      @AuthenticationPrincipal User user);

  @Operation(summary = "매칭 요청 매세지 저장 및 수정")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "저장 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "메세지를 저장할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0004", description = "해당 Id로 매칭 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/matches/{matching-id}/message")
  ResponseEntity<ResultMessageResponseDto> saveMatchingRequestMessage(
      @Valid @RequestBody SaveMatchingRequestMessageRequestDto dto,
      @PathVariable(value = "matching-id") Long matchingId,
      @AuthenticationPrincipal User user);

  @Operation(summary = "매칭 요청 메세지 조회")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "조회 성공", content = @Content(schema = @Schema(implementation = GetMatchingRequestMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "메세지를 조회할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0004", description = "해당 Id로 매칭 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/matches/{matching-id}/message")
  ResponseEntity<GetMatchingRequestMessageResponseDto> getMatchingRequestMessage(
      @PathVariable(value = "matching-id") Long matchingId, @AuthenticationPrincipal User user);

  @Operation(summary = "추천이성 가리기")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PatchMapping("users/{user-id}/recommendations/hide")
  ResponseEntity<?> hideRecommendationUser(
      @Parameter(description = "가리고 싶은 유저의 아이디", example = "5") @PathVariable(value = "user-id") Long recommendedUserId,
      @AuthenticationPrincipal User user
  );

  @Operation(summary = "추천이성 스크랩하기")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0005", description = "유저를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("user/{user-id}/scrap")
  ResponseEntity<ResultMessageResponseDto> scrapUser(
      @Parameter(description = "저장하고 싶은 추천이성 아이디", example = "5") @PathVariable(value = "user-id") Long recommendedUserId,
      @AuthenticationPrincipal User user);

  @Operation(summary = "스크랩된 유저 조회하기")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/user/{user-id}/scrap")
  ResponseEntity<ApiListResponseDto<GetScrappedUserResponseDto>> getScrappedUser(
      @Parameter(description = "요청하는 유저 아이디", example = "5") @PathVariable(value = "user-id") Long userId,
      @AuthenticationPrincipal User user
  );

}
