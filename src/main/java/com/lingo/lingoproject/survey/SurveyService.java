package com.lingo.lingoproject.survey;

import com.lingo.lingoproject.domain.AnsweredSurvey;
import com.lingo.lingoproject.domain.Survey;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.SurveyCategory;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.AnsweredSurveyRepository;
import com.lingo.lingoproject.repository.SurveyRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.survey.dto.GetSurveyResponseDto;
import com.lingo.lingoproject.survey.dto.GetUserSurveyResponseDto;
import com.lingo.lingoproject.survey.dto.UpdateSurveyRequestDto;
import com.lingo.lingoproject.survey.dto.UploadSurveyRequestDto;
import com.lingo.lingoproject.utils.GenericUtils;
import com.lingo.lingoproject.utils.JsonListWrapper;
import com.lingo.lingoproject.utils.RedisUtils;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyService {

  private final SurveyRepository surveyRepository;
  private final GenericUtils genericUtils;
  private final AnsweredSurveyRepository answeredSurveyRepository;
  private final UserRepository userRepository;
  private final RedisUtils redisUtils;

  private final int SIGNUP_NUMBER_OF_SURVEYS = 12;
  private final int MAX_SURVEY_STARTER_DAYS = 10;
  private final int NUMBER_OF_DAILY_SURVEYS = 5;

  public void uploadSurveyExcel(MultipartFile file){
    List<Survey> surveyList = new ArrayList<>();
    try(InputStream is = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(is)){
      Row firstRow = workbook.getSheetAt(0).getRow(0);
      Sheet sheet = workbook.getSheetAt(0);
      sheet.removeRow(firstRow);
      for(Row row : sheet){
        String categoryValue = row.getCell(2).getStringCellValue();
        if (categoryValue.isEmpty()) break;
        if(!genericUtils.isContains(SurveyCategory.values(), categoryValue)){
          throw new RingoException("적절하지 않은 카테고리가 입력되었습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
        }
        SurveyCategory category = SurveyCategory.valueOf(categoryValue);
        Survey survey = Survey.builder()
            .surveyNum((int) (row.getCell(0).getNumericCellValue()))
            .confrontSurveyNum((int) (row.getCell(1).getNumericCellValue()))
            .category(category)
            .content(row.getCell(3).getStringCellValue())
            .purpose(row.getCell(4).getStringCellValue())
            .build();
        surveyList.add(survey);
      }
    }
    catch (Exception e){
      if (e instanceof RingoException re){
        throw re;
      }
      log.error("설문 엑셀 파일 파싱 실패. filename={}", file.getOriginalFilename(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    surveyRepository.saveAll(surveyList);
  }

  public void updateSurvey(UpdateSurveyRequestDto dto, Long surveyId){
    Survey survey = surveyRepository.findById(surveyId)
        .orElseThrow(() -> new RingoException("해당 설문을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    String purpose = dto.purpose();
    String content = dto.content();
    String category = dto.category();

    genericUtils.validateAndSetStringValue(purpose, survey::setPurpose);
    genericUtils.validateAndSetStringValue(content, survey::setContent);
    genericUtils.validateAndSetEnum(category, SurveyCategory.values(), survey::setCategory, SurveyCategory.class);

    surveyRepository.save(survey);
  }

  public List<GetSurveyResponseDto> getSurveys(){
    return surveyRepository.findAll()
        .stream()
        .map(GetSurveyResponseDto::from)
        .toList();
  }

  public void saveSurveyResponse(JsonListWrapper<UploadSurveyRequestDto> responses, User user){
    List<AnsweredSurvey> list = new ArrayList<>();
    Map<Integer, AnsweredSurvey> alreadyAnsweredSurveys = answeredSurveyRepository.findAllByUser(user)
        .stream()
        .collect(Collectors.toMap(AnsweredSurvey::getSurveyNum, Function.identity(), (a, b) -> b));

    // 이미 응답한 설문이 존재하면 응답을 덮어씌운다.
    responses.getList().forEach(r -> {

      // 응답 객체 생성
      AnsweredSurvey answeredSurvey = AnsweredSurvey.builder()
          .user(user)
          .surveyNum(r.surveyNum())
          .answer(r.answer())
          .build();

      // 이미 응답한 설문이 존재하면 id를 설정한다.
      AnsweredSurvey alreadyAnsweredSurvey = alreadyAnsweredSurveys.get(r.surveyNum());
      if (alreadyAnsweredSurvey != null) {
        answeredSurvey.setId(alreadyAnsweredSurvey.getId());
      }

      // 리스트에 저장
      list.add(answeredSurvey);
    });

    answeredSurveyRepository.saveAll(list);
  }
  
  public List<GetSurveyResponseDto> getDailySurveys(User user){

    Long userId = user.getId();

    // 이미 설문을 진행했으면 null을 반환
    LocalDateTime now = LocalDate.now().atStartOfDay();
    boolean isExists = answeredSurveyRepository.existsByUserAndUpdatedAtAfter(user, now);
    if (isExists) {
      return null;
    }

    // 캐시 조회
    if (redisUtils.containsUserDailySurvey(userId.toString())){
      return redisUtils.getUserDailySurvey(userId.toString());
    }

    // 일정기간까지는 정해진 설문을 제공한다.
    int numberOfAnswerSurveys = (int) answeredSurveyRepository.countByUser(user);
    if (numberOfAnswerSurveys < SIGNUP_NUMBER_OF_SURVEYS + NUMBER_OF_DAILY_SURVEYS * MAX_SURVEY_STARTER_DAYS){
      List<Survey> surveys = surveyRepository.findAllBySurveyNumBetween
          (numberOfAnswerSurveys + 1, numberOfAnswerSurveys + NUMBER_OF_DAILY_SURVEYS);
      List<GetSurveyResponseDto> results = surveys.stream()
          .map(GetSurveyResponseDto::from)
          .toList();
      redisUtils.saveUserDailySurvey(userId.toString(), results);
      return results;
    }

    // 무작위로 설문을 제공한다.
    int numberOfSurveys = (int) surveyRepository.count();
    Random random = new Random(System.currentTimeMillis());
    List<Integer> randomSelectedSurveyNums = new ArrayList<>();
    for (int i = 0; i < NUMBER_OF_DAILY_SURVEYS; i++){
      randomSelectedSurveyNums.add(random.nextInt(numberOfSurveys) + 1);
    }
    List<Survey> randomSurveys = surveyRepository.findAllBySurveyNumIn(randomSelectedSurveyNums);
    List<GetSurveyResponseDto> results = randomSurveys.stream()
        .map(GetSurveyResponseDto::from)
        .toList();

    // 캐시 저장
    redisUtils.saveUserDailySurvey(userId.toString(), results);
    return results;
  }

  public List<GetUserSurveyResponseDto> getUserSurveyResponses(User user){
    return answeredSurveyRepository.getUserSurveyResponseDto(user.getId());
  }

}
