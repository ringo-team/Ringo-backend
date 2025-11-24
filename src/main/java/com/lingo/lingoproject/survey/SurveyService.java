package com.lingo.lingoproject.survey;

import com.lingo.lingoproject.domain.AnsweredSurvey;
import com.lingo.lingoproject.domain.Survey;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.SurveyCategory;
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
import java.util.Random;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
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
          throw new RingoException("적절하지 않은 카테고리가 입력되었습니다.", HttpStatus.BAD_REQUEST);
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
    catch (RingoException e){
      throw new RingoException(e.getMessage(), e.getStatus());
    }
    catch (Exception e){
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    surveyRepository.saveAll(surveyList);
  }

  public void updateSurvey(UpdateSurveyRequestDto dto, Long surveyId){
    Survey survey = surveyRepository.findById(surveyId)
        .orElseThrow(() -> new RingoException("해당 설문을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    if(dto.purpose() != null && !dto.purpose().isEmpty()) survey.setPurpose(dto.purpose());
    if(dto.content() != null && !dto.content().isEmpty()) survey.setContent(dto.content());
    if(dto.category() != null && genericUtils.isContains(SurveyCategory.values(),dto.category()))
      survey.setCategory(SurveyCategory.valueOf(dto.category()));
    surveyRepository.save(survey);
  }

  public List<GetSurveyResponseDto> getSurveys(){
    return surveyRepository.findAll()
        .stream()
        .map(s -> {
              return GetSurveyResponseDto.builder()
                  .surveyNum(s.getSurveyNum())
                  .confrontSurveyNum(s.getConfrontSurveyNum())
                  .content(s.getContent())
                  .category(s.getCategory().toString())
                  .purpose(s.getPurpose())
                  .build();
            }
        )
        .toList();
  }

  public void saveSurveyResponse(JsonListWrapper<UploadSurveyRequestDto> responses, User user){
    List<AnsweredSurvey> list = new ArrayList<>();
    responses.getList().forEach(r -> {
      AnsweredSurvey answeredSurvey = AnsweredSurvey.builder()
          .user(user)
          .surveyNum(r.surveyNum())
          .answer(r.answer())
          .build();
      list.add(answeredSurvey);
    });
    List<AnsweredSurvey> alreadyAnsweredSurveys = answeredSurveyRepository.findAllByUser(user);

    for (AnsweredSurvey answeredSurvey : list){
      Integer answeredSurveyNum = answeredSurvey.getSurveyNum();
      for (AnsweredSurvey alreadyAnsweredSurvey : alreadyAnsweredSurveys){
        if (answeredSurveyNum.equals(alreadyAnsweredSurvey.getSurveyNum())){
          answeredSurvey.setId(alreadyAnsweredSurvey.getId());
          break;
        }
      }
    }

    answeredSurveyRepository.saveAll(list);
  }
  
  public List<GetSurveyResponseDto> getDailySurveys(Long userId){
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("해당 요청의 유저가 존재하지 않습니다.", HttpStatus.BAD_REQUEST));
    LocalDateTime now = LocalDate.now().atStartOfDay();
    boolean isExists = answeredSurveyRepository.existsByUserAndCreatedAtAfter(user, now);
    if (isExists) {
      return null;
    }
    if (redisUtils.containsUserDailySurvey(userId.toString())){
      return redisUtils.getUserDailySurvey(userId.toString());
    }
    int numberOfAnswerSurveys = (int) answeredSurveyRepository.countByUser(user);
    // 일정기간까지는 정해진 설문을 응답한다.
    if (numberOfAnswerSurveys < SIGNUP_NUMBER_OF_SURVEYS + NUMBER_OF_DAILY_SURVEYS * MAX_SURVEY_STARTER_DAYS){
      List<Survey> surveys = surveyRepository.findAllBySurveyNumBetween(numberOfAnswerSurveys + 1, numberOfAnswerSurveys + NUMBER_OF_DAILY_SURVEYS);
      List<GetSurveyResponseDto> results = surveys.stream().map(
          e -> GetSurveyResponseDto.builder()
              .surveyNum(e.getSurveyNum())
              .confrontSurveyNum(e.getConfrontSurveyNum())
              .category(e.getCategory().toString())
              .content(e.getContent())
              .purpose(e.getPurpose())
              .build()
      ).toList();
      redisUtils.saveUserDailySurvey(userId.toString(), results);
      return results;
    }
    int numberOfSurveys = (int) surveyRepository.count();
    Random random = new Random(System.currentTimeMillis());
    List<Integer> randomSelectedSurveyNums = new ArrayList<>();
    for (int i = 0; i < NUMBER_OF_DAILY_SURVEYS; i++){
      randomSelectedSurveyNums.add(random.nextInt(numberOfSurveys) + 1);
    }
    List<Survey> randomSurveys = surveyRepository.findAllBySurveyNumIn(randomSelectedSurveyNums);
    List<GetSurveyResponseDto> results = randomSurveys.stream().map(
        e -> GetSurveyResponseDto.builder()
            .surveyNum(e.getSurveyNum())
            .confrontSurveyNum(e.getConfrontSurveyNum())
            .category(e.getCategory().toString())
            .content(e.getContent())
            .purpose(e.getPurpose())
            .build()
    ).toList();
    redisUtils.saveUserDailySurvey(userId.toString(), results);
    return results;
  }

  public List<GetUserSurveyResponseDto> getUserSurveyResponses(User user){
    return answeredSurveyRepository.getUserSurveyResponseDto(user.getId());
  }

}
