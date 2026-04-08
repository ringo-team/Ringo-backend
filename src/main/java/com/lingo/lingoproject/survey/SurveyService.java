package com.lingo.lingoproject.survey;

import com.lingo.lingoproject.domain.AnsweredSurvey;
import com.lingo.lingoproject.domain.Survey;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.SurveyCategory;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.AnsweredSurveyRepository;
import com.lingo.lingoproject.repository.SurveyRepository;
import com.lingo.lingoproject.survey.dto.GetSurveyResponseDto;
import com.lingo.lingoproject.survey.dto.GetUserSurveyResponseDto;
import com.lingo.lingoproject.survey.dto.UpdateSurveyRequestDto;
import com.lingo.lingoproject.survey.dto.UploadSurveyRequestDto;
import com.lingo.lingoproject.utils.GenericUtils;
import com.lingo.lingoproject.utils.ApiListResponseDto;
import com.lingo.lingoproject.utils.RedisUtils;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyService {

  private final SurveyRepository surveyRepository;
  private final AnsweredSurveyRepository answeredSurveyRepository;
  private final RedisUtils redisUtils;

  private final int SIGNUP_NUMBER_OF_SURVEYS = 12;
  private final int MAX_SURVEY_STARTER_DAYS = 10;
  private final int NUMBER_OF_DAILY_SURVEYS = 5;
  private final RedisTemplate<String, Object> redisTemplate;

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

        SurveyCategory category = GenericUtils.validateAndReturnEnumValue(SurveyCategory.values(), categoryValue);
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

    GenericUtils.validateAndSetStringValue(purpose, survey::setPurpose);
    GenericUtils.validateAndSetStringValue(content, survey::setContent);
    GenericUtils.validateAndSetEnum(category, SurveyCategory.values(), survey::setCategory);

    surveyRepository.save(survey);
  }

  public List<GetSurveyResponseDto> getSurveys(){
    return surveyRepository.findAll()
        .stream()
        .map(GetSurveyResponseDto::from)
        .toList();
  }

  public void saveSurveyResponse(ApiListResponseDto<UploadSurveyRequestDto> responses, User user){

    List<AnsweredSurvey> list = new ArrayList<>();

    // 이미 응답한 설문이 존재하면 응답을 덮어씌운다.
    responses.getList().forEach(r -> {

      // 이미 응답한 설문이 존재하면 수정하고 list에 추가
      if (answeredSurveyRepository.existsByUserAndSurveyNum(user, r.surveyNum())){
        AnsweredSurvey answeredSurvey = answeredSurveyRepository.findByUserAndSurveyNum(user, r.surveyNum());
        log.info("""
            user-id: {},
            survey-num: {},
            answer-survey-변화: {} ------> {}
            """,
            user.getId(),
            r.surveyNum(),
            answeredSurvey.getAnswer(),
            r.answer()
        );
        answeredSurvey.setAnswer(r.answer());
        list.add(answeredSurvey);
      }
      else {
        AnsweredSurvey answeredSurvey = AnsweredSurvey.builder()
            .user(user)
            .surveyNum(r.surveyNum())
            .answer(r.answer())
            .build();
        log.info("""
            user-id: {},
            survey-num: {},
            answer-survey-변화: {} ------> {}
            """,
            user.getId(),
            r.surveyNum(),
            answeredSurvey.getAnswer(),
            r.answer()
            );
        list.add(answeredSurvey);
      }
    });

    answeredSurveyRepository.saveAll(list);
  }
  
  public List<GetSurveyResponseDto> getDailySurveys(User user){

    Long userId = user.getId();

    // 이미 설문을 진행했으면 null을 반환
    LocalDateTime now = LocalDate.now().atStartOfDay();
    if (answeredSurveyRepository.existsByUserAndUpdatedAtAfter(user, now)) {
      return null;
    }

    // 캐시 조회
    if (redisTemplate.hasKey("dailySurvey::" + user.getId())){
      return redisUtils.getUserDailySurvey(userId.toString());
    }

    // 일정기간까지는 정해진 설문을 제공한다.
    int numberOfAnswerSurveys = (int) answeredSurveyRepository.countByUser(user);
    if (numberOfAnswerSurveys < SIGNUP_NUMBER_OF_SURVEYS + NUMBER_OF_DAILY_SURVEYS * MAX_SURVEY_STARTER_DAYS){
      List<GetSurveyResponseDto> results = surveyRepository.findAllBySurveyNumBetween
          (numberOfAnswerSurveys + 1, numberOfAnswerSurveys + NUMBER_OF_DAILY_SURVEYS)
          .stream()
          .map(GetSurveyResponseDto::from)
          .toList();
      log.info("""
          number-of-answer-surveys: {},
          limit: {},
          조회된_설문_번호: {} ~ {}
          총_조회된_설문_개수: {} 개
          """,
          numberOfAnswerSurveys,
          SIGNUP_NUMBER_OF_SURVEYS + NUMBER_OF_DAILY_SURVEYS * MAX_SURVEY_STARTER_DAYS,
          numberOfAnswerSurveys + 1, numberOfAnswerSurveys + NUMBER_OF_DAILY_SURVEYS,
          results.size()
          );
      redisUtils.cacheUntilMidnight("recommend-for-daily-survey::" + userId, new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), results));

      results.forEach(result -> {
          log.info("""
              user-id: {},
              survey-num: {},
              confront-survey-num: {},
              survey-purpose: {}]
              """,
              user.getId(),
              result.surveyNum(),
              result.confrontSurveyNum(),
              result.purpose()
              );
      });

      return results;
    }

    List<GetSurveyResponseDto> results = getRandomlySelectedDailySurvey();

    results.forEach(result -> {
          log.info("""
                  user-id: {},
                  survey-num: {},
                  confront-survey-num: {},
                  survey-purpose: {}]
                  """,
              user.getId(),
              result.surveyNum(),
              result.confrontSurveyNum(),
              result.purpose()
          );
        });


    // 캐시 저장
    redisUtils.cacheUntilMidnight("dailySurvey::" + userId, new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), results));

    return results;
  }

  private List<GetSurveyResponseDto> getRandomlySelectedDailySurvey(){

    int totalCountOfSurveys = (int) surveyRepository.count();
    Set<Integer> randomlySelectedSurveyNums = new HashSet<>();

    ThreadLocalRandom random = ThreadLocalRandom.current();
    while (randomlySelectedSurveyNums.size() < NUMBER_OF_DAILY_SURVEYS){
      randomlySelectedSurveyNums.add(random.nextInt(totalCountOfSurveys) + 1);
    }

    log.info("""
        total-survey-count: {},
        randomly-selected-survey-nums: {}
        """, totalCountOfSurveys, randomlySelectedSurveyNums);

    List<Survey> randomSurveys = surveyRepository.findAllBySurveyNumIn(randomlySelectedSurveyNums);
    return randomSurveys.stream()
        .map(GetSurveyResponseDto::from)
        .toList();
  }

  public List<GetUserSurveyResponseDto> getUserSurveyResponses(User user){
    return answeredSurveyRepository.getUserSurveyResponseDto(user.getId());
  }

}
