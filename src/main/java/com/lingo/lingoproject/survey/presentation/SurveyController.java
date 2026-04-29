package com.lingo.lingoproject.survey.presentation;
import com.lingo.lingoproject.survey.application.SurveyService;import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.Role;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.survey.presentation.dto.GetSurveyResponseDto;
import com.lingo.lingoproject.survey.presentation.dto.GetUserSurveyResponseDto;
import com.lingo.lingoproject.survey.presentation.dto.UpdateSurveyRequestDto;
import com.lingo.lingoproject.survey.presentation.dto.UploadSurveyRequestDto;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SurveyController implements SurveyApi{

  private final SurveyService surveyService;

  public ResponseEntity<ResultMessageResponseDto> uploadSurveyExcel(MultipartFile file){
    log.info("step=설문지_업로드_시작");
    surveyService.importSurveysFromExcel(file);
    log.info("step=설문지_업로드_완료");

    return ResponseEntity.status(HttpStatus.CREATED).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "성공적으로 업로드 되었습니다."));
  }

  public ResponseEntity<ResultMessageResponseDto> updateSurvey(UpdateSurveyRequestDto dto, Long surveyId, User user){
    if(!user.getRole().equals(Role.ADMIN)){
      throw new RingoException("관리자만 설문지를 수정할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
    log.info("step=설문지_수정_시작, userId={}, surveyId={}", user.getId(), surveyId);
    surveyService.updateSurvey(dto, surveyId);
    log.info("step=설문지_수정_완료, userId={}, surveyId={}", user.getId(), surveyId);

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "성공적으로 수정하였습니다."));
  }

  public ResponseEntity<ApiListResponseDto<GetSurveyResponseDto>> getSurveys(){
    log.info("step=설문지_조회_시작");
    List<GetSurveyResponseDto> surveys = surveyService.findAllSurveys();
    log.info("step=설문지_조회_완료");

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), surveys));
  }

  public ResponseEntity<ResultMessageResponseDto> saveSurveyResponse(
      ApiListResponseDto<UploadSurveyRequestDto> responses,
      Long userId,
      User user){
    if (!user.getId().equals(userId)){
      throw new RingoException("설문에 응답할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    log.info("step=설문_응답_저장_시작, userId={}", user.getId());
    surveyService.upsertSurveyResponses(responses, user);
    log.info("step=설문_응답_저장_완료, userId={}", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "정상적으로 설문지 응답이 저장되었습니다."));
  }

  public ResponseEntity<ApiListResponseDto<GetSurveyResponseDto>> getDailySurveys(
      Long userId, User user
  ){
    if (!user.getId().equals(userId)){
      throw new RingoException("일일 설문 조회할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    log.info("step=일일_설문_조회_시작, userId={}", userId);
    List<GetSurveyResponseDto> dailySurveys = surveyService.getDailySurveys(user);
    log.info("step=일일_설문_조회_완료, userId={}", userId);

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), dailySurveys));
  }

  public ResponseEntity<ApiListResponseDto<GetUserSurveyResponseDto>> getUserSurveyResponses(Long userId, User user){
    if (!(userId.equals(user.getId()) || user.getRole().equals(Role.ADMIN))) {
      throw new RingoException("설문 응답 결과를 조회할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

    log.info("step=유저_설문_응답_조회_시작, userId={}", user.getId());
    List<GetUserSurveyResponseDto> userSurveyResponses = surveyService.findUserSurveyResponses(user);
    log.info("step=유저_설문_응답_조회_완료, userId={}", user.getId());

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), userSurveyResponses));
  }

  @Override
  public ResponseEntity<ApiListResponseDto<GetSurveyResponseDto>> resetAndGetInitialSurveys(User user) {
    List<GetSurveyResponseDto> list = surveyService.resetAndGetInitialSurveys(user);
    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), list));
  }
}
