package com.lingo.lingoproject.survey.application;

import com.lingo.lingoproject.shared.domain.model.AnsweredSurvey;
import com.lingo.lingoproject.shared.domain.model.Survey;
import com.lingo.lingoproject.shared.domain.model.SurveyCategory;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.AnsweredSurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.SurveyRepository;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.GenericUtils;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import com.lingo.lingoproject.survey.presentation.dto.GetSurveyResponseDto;
import com.lingo.lingoproject.survey.presentation.dto.GetUserSurveyResponseDto;
import com.lingo.lingoproject.survey.presentation.dto.UpdateSurveyRequestDto;
import com.lingo.lingoproject.survey.presentation.dto.UploadSurveyRequestDto;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
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

/**
 * 설문 항목 관리 및 사용자 설문 응답 처리 비즈니스 로직을 담당하는 서비스.
 *
 * <h2>일일 설문 제공 전략</h2>
 * <p>사용자의 누적 응답 수({@code answeredSurveyCount})가 임계값({@code SEQUENTIAL_SURVEY_THRESHOLD})
 * 미만이면 순차적으로 설문을 제공하고, 임계값 이상이면 랜덤으로 제공합니다.</p>
 * <pre>
 *   answeredSurveyCount < INITIAL_SURVEY_COUNT + DAILY * SEQUENTIAL_PERIOD
 *     → 순차 제공 (answeredCount + 1 ~ answeredCount + 5번 설문)
 *   answeredSurveyCount >= 임계값
 *     → 랜덤 5개 제공
 * </pre>
 *
 * <h2>캐싱</h2>
 * <p>일일 설문 결과는 {@code dailySurvey::{userId}} 키로 자정까지 Redis에 캐싱됩니다.
 * 오늘 이미 답변한 경우 null을 반환하여 중복 노출을 방지합니다.</p>
 *
 * <h2>설문 응답 저장 방식</h2>
 * <p>upsert 방식을 사용합니다. 동일 설문번호에 이미 응답이 있으면 덮어쓰고,
 * 없으면 새로 생성합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SurveyService {

  /** 초기 순차 제공 설문 개수. 가입 후 처음 응답해야 하는 기본 설문 수. */
  private static final int INITIAL_SURVEY_COUNT = 12;
  /** 순차 일일 설문 제공 기간 (일). */
  private static final int SEQUENTIAL_SURVEY_PERIOD_DAYS = 10;
  /** 하루에 제공하는 설문 개수. */
  private static final int DAILY_SURVEY_COUNT = 5;
  /** 순차 제공에서 랜덤 제공으로 전환되는 누적 응답 수 임계값. */
  private static final int SEQUENTIAL_SURVEY_THRESHOLD =
      INITIAL_SURVEY_COUNT + DAILY_SURVEY_COUNT * SEQUENTIAL_SURVEY_PERIOD_DAYS;

  private final SurveyRepository surveyRepository;
  private final AnsweredSurveyRepository answeredSurveyRepository;
  private final RedisUtils redisUtils;
  private final RedisTemplate<String, Object> redisTemplate;

  /**
   * Excel 파일을 파싱해 설문 항목을 일괄 저장합니다.
   */
  public void importSurveysFromExcel(MultipartFile file) {
    List<Survey> surveys = parseExcelToSurveys(file);
    surveyRepository.saveAll(surveys);
  }

  /**
   * 설문 항목을 수정한다.
   *
   * <p>null이 아닌 필드만 선택적으로 업데이트합니다.</p>
   *
   * @param dto      수정할 내용이 담긴 DTO (purpose, content, category)
   * @param surveyId 수정할 설문 ID
   * @throws RingoException 해당 설문이 존재하지 않는 경우
   */
  public void updateSurvey(UpdateSurveyRequestDto dto, Long surveyId) {
    Survey survey = surveyRepository.findById(surveyId)
        .orElseThrow(() -> new RingoException("해당 설문을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    GenericUtils.validateAndSetStringValue(dto.purpose(), survey::setPurpose);
    GenericUtils.validateAndSetStringValue(dto.content(), survey::setContent);
    GenericUtils.validateAndSetEnum(dto.category(), SurveyCategory.values(), survey::setCategory);

    surveyRepository.save(survey);
  }

  /**
   * 전체 설문 항목 목록을 반환한다.
   *
   * @return 모든 설문 항목 DTO 목록
   */
  public List<GetSurveyResponseDto> findAllSurveys() {
    return surveyRepository.findAll()
        .stream()
        .map(GetSurveyResponseDto::from)
        .toList();
  }

  /**
   * 설문 응답을 upsert 방식으로 저장합니다.
   * 이미 응답한 설문이 있으면 덮어쓰고, 없으면 새로 생성합니다.
   */
  public void upsertSurveyResponses(ApiListResponseDto<UploadSurveyRequestDto> responses, User user) {
    List<AnsweredSurvey> surveyResponses = responses.getList().stream()
        .map(response -> upsertSingleResponse(user, response))
        .toList();

    answeredSurveyRepository.saveAll(surveyResponses);
  }

  /**
   * 당일 일일 설문을 반환합니다.
   * - 오늘 이미 응답했으면 null 반환
   * - 누적 응답 수가 임계값 미만이면 순차 설문 제공
   * - 임계값 이상이면 랜덤 설문 제공 (자정까지 캐싱)
   */
  public List<GetSurveyResponseDto> fetchDailySurveys(User user) {
    if (hasCompletedTodaySurveys(user)) {
      return null;
    }

    List<GetSurveyResponseDto> cached = getCachedDailySurveys(user.getId());
    if (cached != null) {
      return cached;
    }

    int answeredSurveyCount = (int) answeredSurveyRepository.countByUser(user);
    if (answeredSurveyCount < SEQUENTIAL_SURVEY_THRESHOLD) {
      return fetchSequentialDailySurveys(user, answeredSurveyCount);
    }

    return fetchRandomDailySurveysAndCache(user);
  }

  /**
   * 특정 사용자가 응답한 설문 목록을 반환한다.
   *
   * @param user 조회할 사용자
   * @return 해당 사용자의 설문 응답 목록 (설문번호, 응답값 포함)
   */
  public List<GetUserSurveyResponseDto> findUserSurveyResponses(User user) {
    return answeredSurveyRepository.getUserSurveyResponseDto(user.getId());
  }

  // ─── private helpers ───────────────────────────────────────────────────────

  /**
   * 엑셀 파일을 파싱하여 {@link Survey} 엔티티 목록으로 변환한다.
   *
   * <p>0번 시트의 첫 번째 행(헤더)을 제거한 뒤 각 행을 설문으로 변환합니다.
   * 카테고리 셀이 비어 있는 행에서 파싱을 중단합니다.</p>
   *
   * @param file 파싱할 엑셀 파일
   * @return 파싱된 설문 엔티티 목록
   * @throws RingoException 파싱 중 예외가 발생한 경우
   */
  private List<Survey> parseExcelToSurveys(MultipartFile file) {
    List<Survey> surveys = new ArrayList<>();
    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {

      Sheet sheet = workbook.getSheetAt(0);
      sheet.removeRow(sheet.getRow(0)); // 헤더 행 제거

      for (Row row : sheet) {
        String categoryValue = row.getCell(2).getStringCellValue();
        if (categoryValue.isEmpty()) break;
        surveys.add(buildSurveyFromRow(row, categoryValue));
      }
    } catch (Exception e) {
      if (e instanceof RingoException re) throw re;
      log.error("step=설문_엑셀_파싱_실패, filename={}", file.getOriginalFilename(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return surveys;
  }

  /** 엑셀 행 데이터를 {@link Survey} 엔티티로 변환한다. */
  private Survey buildSurveyFromRow(Row row, String categoryValue) {
    SurveyCategory category = GenericUtils.validateAndReturnEnumValue(SurveyCategory.values(), categoryValue);
    return Survey.of(
        (int) row.getCell(0).getNumericCellValue(),
        (int) row.getCell(1).getNumericCellValue(),
        category,
        row.getCell(3).getStringCellValue(),
        row.getCell(4).getStringCellValue());
  }

  /**
   * 단일 설문 응답을 upsert 처리한다.
   *
   * <p>이미 응답이 있으면 기존 엔티티의 answer를 수정하고,
   * 없으면 새 {@link AnsweredSurvey} 엔티티를 생성합니다.</p>
   */
  private AnsweredSurvey upsertSingleResponse(User user, UploadSurveyRequestDto response) {
    if (answeredSurveyRepository.existsByUserAndSurveyNum(user, response.surveyNum())) {
      AnsweredSurvey existing = answeredSurveyRepository.findByUserAndSurveyNum(user, response.surveyNum());
      log.info("step=설문_응답_수정, userId={}, surveyNum={}, before={}, after={}",
          user.getId(), response.surveyNum(), existing.getAnswer(), response.answer());
      existing.setAnswer(response.answer());
      return existing;
    }

    log.info("step=설문_응답_신규저장, userId={}, surveyNum={}, answer={}",
        user.getId(), response.surveyNum(), response.answer());
    return AnsweredSurvey.of(user, response.surveyNum(), response.answer());
  }

  /** 오늘(자정 이후) 이미 설문에 응답한 사용자인지 확인한다. */
  private boolean hasCompletedTodaySurveys(User user) {
    LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
    return answeredSurveyRepository.existsByUserAndUpdatedAtAfter(user, startOfToday);
  }

  /** Redis에서 캐싱된 일일 설문을 가져온다. 캐시가 없으면 null을 반환한다. */
  private List<GetSurveyResponseDto> getCachedDailySurveys(Long userId) {
    if (Boolean.TRUE.equals(redisTemplate.hasKey("dailySurvey::" + userId))) {
      return redisUtils.getUserDailySurvey(userId.toString());
    }
    return null;
  }

  /**
   * 순차 일일 설문을 조회하고 Redis에 캐싱한다.
   *
   * <p>누적 응답 수를 기준으로 다음 5개 설문을 순서대로 제공합니다.
   * 결과는 자정까지 Redis에 캐싱됩니다.</p>
   */
  private List<GetSurveyResponseDto> fetchSequentialDailySurveys(User user, int answeredSurveyCount) {
    int from = answeredSurveyCount + 1;
    int to = answeredSurveyCount + DAILY_SURVEY_COUNT;

    List<GetSurveyResponseDto> surveys = surveyRepository.findAllBySurveyNumBetween(from, to)
        .stream()
        .map(GetSurveyResponseDto::from)
        .toList();

    log.info("step=순차_일일설문_조회, userId={}, answeredCount={}, range={}~{}, resultCount={}",
        user.getId(), answeredSurveyCount, from, to, surveys.size());
    logDailySurveyDetails(user.getId(), surveys);

    redisUtils.cacheUntilMidnight(
        "recommend-for-daily-survey::" + user.getId(),
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), surveys)
    );
    return surveys;
  }

  /**
   * 랜덤 일일 설문을 선택하고 Redis에 캐싱한다.
   *
   * <p>전체 설문 중 중복 없이 {@code DAILY_SURVEY_COUNT}개를 무작위 선택합니다.
   * 결과는 자정까지 Redis에 캐싱됩니다.</p>
   */
  private List<GetSurveyResponseDto> fetchRandomDailySurveysAndCache(User user) {
    List<GetSurveyResponseDto> surveys = selectRandomDailySurveys();

    log.info("step=랜덤_일일설문_조회, userId={}, resultCount={}", user.getId(), surveys.size());
    logDailySurveyDetails(user.getId(), surveys);

    redisUtils.cacheUntilMidnight(
        "dailySurvey::" + user.getId(),
        new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), surveys)
    );
    return surveys;
  }

  /**
   * 전체 설문에서 중복 없이 {@code DAILY_SURVEY_COUNT}개의 설문을 무작위 선택한다.
   *
   * <p>{@link java.util.HashSet}을 이용해 중복 없이 설문번호를 선택합니다.</p>
   */
  private List<GetSurveyResponseDto> selectRandomDailySurveys() {
    int totalSurveyCount = (int) surveyRepository.count();
    Set<Integer> selectedSurveyNums = new HashSet<>();
    ThreadLocalRandom random = ThreadLocalRandom.current();

    while (selectedSurveyNums.size() < DAILY_SURVEY_COUNT) {
      selectedSurveyNums.add(random.nextInt(totalSurveyCount) + 1);
    }

    log.info("step=랜덤_설문번호_선택, totalCount={}, selected={}", totalSurveyCount, selectedSurveyNums);

    return surveyRepository.findAllBySurveyNumIn(selectedSurveyNums)
        .stream()
        .map(GetSurveyResponseDto::from)
        .toList();
  }

  /** 제공된 일일 설문 항목의 상세 정보를 로그로 출력한다. */
  private void logDailySurveyDetails(Long userId, List<GetSurveyResponseDto> surveys) {
    surveys.forEach(survey -> log.info(
        "step=일일설문_상세, userId={}, surveyNum={}, confrontSurveyNum={}, purpose={}",
        userId, survey.surveyNum(), survey.confrontSurveyNum(), survey.purpose()
    ));
  }
}
