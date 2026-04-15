package com.lingo.lingoproject.survey.presentation;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.survey.presentation.dto.GetSurveyResponseDto;
import com.lingo.lingoproject.survey.presentation.dto.GetUserSurveyResponseDto;
import com.lingo.lingoproject.survey.presentation.dto.UpdateSurveyRequestDto;
import com.lingo.lingoproject.survey.presentation.dto.UploadSurveyRequestDto;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "survey-controller", description = "설문지/설문결과 저장/조회/업데이트 관련 api")
public interface SurveyApi {
  @Operation(summary = "설문지 업로드")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "업로드 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0007", description = "적절하지 않은 카테고리가 입력되었습니다", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping(value = "/surveys", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<ResultMessageResponseDto> uploadSurveyExcel(@RequestParam("file") MultipartFile file);

  @Operation(summary = "설문지 수정")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "업로드 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0004", description = "해당 id로 설문 객체를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PatchMapping("/surveys/{survey-id}")
  ResponseEntity<ResultMessageResponseDto> updateSurvey(
      @RequestBody UpdateSurveyRequestDto dto,
      @PathVariable(value = "survey-id") Long surveyId,
      @AuthenticationPrincipal User user);

  @Operation(summary = "설문지 조회")
  @GetMapping("/surveys")
  ResponseEntity<ApiListResponseDto<GetSurveyResponseDto>> getSurveys();

  @Operation(summary = "설문지 응답 저장", description = "유저가 진행한 설문지 응답 저장")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "저장 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "설문에 응답할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping("/users/{user-id}/surveys/responses")
  ResponseEntity<ResultMessageResponseDto> saveSurveyResponse(
      @RequestBody ApiListResponseDto<UploadSurveyRequestDto> responses,
      @PathVariable(value = "user-id") Long userId,
      @AuthenticationPrincipal User user);

  @Operation(summary = "일일 설문 조회", description = "날마다 진행하는 설문 문항들 조회, 만약 설문을 진행했으면 null 반환")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "조회 성공"),
          @ApiResponse(responseCode = "E0003", description = "일일 설문 조회할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/users/{user-id}/surveys/daily")
  ResponseEntity<ApiListResponseDto<GetSurveyResponseDto>> getDailySurveys(
      @PathVariable(value = "user-id") Long userId,
      @AuthenticationPrincipal User user
  );

  @Operation(summary = "유저가 응답한 설문 조회", description = "유저가 그동안 응답한 모든 설문 응답들을 조회")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "0000", description = "조회 성공"),
          @ApiResponse(responseCode = "E0003", description = "설문 응답 결과를 조회할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("users/{user-id}/surveys")
  ResponseEntity<ApiListResponseDto<GetUserSurveyResponseDto>> getUserSurveyResponses(
      @PathVariable(value = "user-id") Long userId, @AuthenticationPrincipal User user
  );
}
