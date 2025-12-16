package com.lingo.lingoproject.survey;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.security.dto.LoginResponseDto;
import com.lingo.lingoproject.survey.dto.GetSurveyResponseDto;
import com.lingo.lingoproject.survey.dto.GetUserSurveyResponseDto;
import com.lingo.lingoproject.survey.dto.UpdateSurveyRequestDto;
import com.lingo.lingoproject.survey.dto.UploadSurveyRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SurveyController {

  private final SurveyService surveyService;

  @Operation(summary = "설문지 업로드")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "업로드 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0007",
              description = "적절하지 않은 카테고리가 입력되었습니다",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping(value = "/surveys", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ResultMessageResponseDto> uploadSurveyExcel(@RequestParam("file")MultipartFile file){
    try {
      log.info("step=설문지_업로드_시작, status=SUCCESS");
      surveyService.uploadSurveyExcel(file);
      log.info("step=설문지_업로드_완료, status=SUCCESS");

      return ResponseEntity.status(HttpStatus.CREATED).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "성공적으로 업로드 되었습니다."));

    } catch (Exception e) {
      log.error("step=설문지_업로드_실패, status=FAILED", e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("설문지 업로드에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "설문지 수정")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "업로드 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0004",
              description = "해당 id로 설문 객체를 찾을 수 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PatchMapping("/surveys/{surveyId}")
  public ResponseEntity<ResultMessageResponseDto> updateSurvey(
      @RequestBody UpdateSurveyRequestDto dto,
      @PathVariable(value = "surveyId") Long surveyId,
      @AuthenticationPrincipal User user){
    if(!user.getRole().equals(Role.ADMIN)){
      throw new RingoException("관리자만 설문지를 수정할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
    try {
      log.info("userId={}, surveyId={}, step=설문지_수정_시작, status=SUCCESS", user.getId(), surveyId);
      surveyService.updateSurvey(dto, surveyId);
      log.info("userId={}, surveyId={}, step=설문지_수정_완료, status=SUCCESS", user.getId(), surveyId);

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "성공적으로 수정하였습니다."));

    } catch (Exception e) {
      log.error("userId={}, surveyId={}, step=설문지_수정_실패, status=FAILED", user.getId(), surveyId, e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("설문지 수정에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "설문지 조회")
  @GetMapping("/surveys")
  public ResponseEntity<JsonListWrapper<GetSurveyResponseDto>> getSurveys(){
    try {
      log.info("step=설문지_조회_시작, status=SUCCESS");
      List<GetSurveyResponseDto> list = surveyService.getSurveys();
      log.info("step=설문지_조회_완료, status=SUCCESS");

      return ResponseEntity.status(HttpStatus.OK)
          .body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), list));
    } catch (Exception e) {
      log.error("step=설문지_조회_실패, status=FAILED", e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("설문지를 조회하는데 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "설문지 응답 저장", description = "유저가 진행한 설문지 응답 저장")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "저장 성공",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "설문에 응답할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @PostMapping("/users/{userId}/surveys/responses")
  public ResponseEntity<ResultMessageResponseDto> saveSurveyResponse(
      @RequestBody JsonListWrapper<UploadSurveyRequestDto> responses,
      @PathVariable(value = "userId") Long userId,
      @AuthenticationPrincipal User user){
    try {

      if (!user.getId().equals(userId)){
        throw new RingoException("설문에 응답할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
      }

      log.info("userId={}, step=설문_응답_저장_시작, status=SUCCESS", user.getId());
      surveyService.saveSurveyResponse(responses, user);
      log.info("userId={}, step=설문_응답_저장_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
          ErrorCode.SUCCESS.getCode(), "정상적으로 설문지 응답이 저장되었습니다."));
    } catch (Exception e) {
      log.error("userId={}, step=설문_응답_저장_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("설문 응답 저장에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "일일 설문 조회", description = "날마다 진행하는 설문 문항들 조회, 만약 설문을 진행했으면 null 반환")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "조회 성공"
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "일일 설문 조회할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("/users/{userId}/surveys/daily")
  public ResponseEntity<JsonListWrapper<GetSurveyResponseDto>> getDailySurveys(
      @PathVariable(value = "userId") Long userId,
      @AuthenticationPrincipal User user
  ){
    try {
      if (!user.getId().equals(userId)){
        throw new RingoException("일일 설문 조회할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
      }

      log.info("userId={}, step=일일_설문_조회_시작, status=SUCCESS", userId);
      List<GetSurveyResponseDto> dto = surveyService.getDailySurveys(user);
      log.info("userId={}, step=일일_설문_조회_완료, status=SUCCESS", userId);

      return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), dto));
    } catch (Exception e) {
      log.error("userId={}, step=일일_설문_조회_실패, status=FAILED", userId, e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("일일 설문 조회에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @Operation(summary = "유저가 응답한 설문 조회", description = "유저가 그동안 응답한 모든 설문 응답들을 조회")
  @ApiResponses(
      value = {
          @ApiResponse(
              responseCode = "0000",
              description = "조회 성공"
          ),
          @ApiResponse(
              responseCode = "E0003",
              description = "설문 응답 결과를 조회할 권한이 없습니다.",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          ),
          @ApiResponse(
              responseCode = "E1000",
              description = "내부 오류, 기타 문의",
              content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))
          )
      }
  )
  @GetMapping("users/{userId}/surveys")
  public ResponseEntity<JsonListWrapper<GetUserSurveyResponseDto>> getUserSurveyResponses(
      @PathVariable Long userId, @AuthenticationPrincipal User user
  ){
    try {
      if (!(userId.equals(user.getId()) || user.getRole().equals(Role.ADMIN))) {
        throw new RingoException("설문 응답 결과를 조회할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
      }

      log.info("userId={}, step=유저_설문_응답_조회_시작, status=SUCCESS", user.getId());
      List<GetUserSurveyResponseDto> result = surveyService.getUserSurveyResponses(user);
      log.info("userId={}, step=유저_설문_응답_조회_완료, status=SUCCESS", user.getId());

      return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(ErrorCode.SUCCESS.getCode(), result));
    } catch (Exception e) {
      log.error("userId={}, step=유저_설문_응답_조회_실패, status=FAILED", user.getId(), e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("유저 설문 응답 조회에 실패했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }
}
