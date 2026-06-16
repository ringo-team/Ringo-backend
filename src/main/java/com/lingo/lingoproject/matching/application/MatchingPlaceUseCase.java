package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.community.presentation.dto.GetPlaceDetailResponseDto;
import com.lingo.lingoproject.matching.domain.service.RecommendationDomainService;
import com.lingo.lingoproject.matching.presentation.dto.GetTypePlaceRequestDto;
import com.lingo.lingoproject.shared.domain.elastic.PlaceDocument;
import com.lingo.lingoproject.shared.domain.model.AnsweredSurvey;
import com.lingo.lingoproject.shared.domain.model.Keyword;
import com.lingo.lingoproject.shared.domain.model.Place;
import com.lingo.lingoproject.shared.domain.model.PlaceType;
import com.lingo.lingoproject.shared.domain.model.PlaceTypeEntity;
import com.lingo.lingoproject.shared.domain.model.Survey;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserScrapPlace;
import com.lingo.lingoproject.shared.infrastructure.elastic.PlaceSearchRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.AnsweredSurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.KeywordRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PlaceRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PlaceTypeRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.SurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserScrapPlaceRepository;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.utils.RedisKey;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingPlaceUseCase {

  private final UserQueryUseCase userQueryUseCase;
  private final AnsweredSurveyRepository answeredSurveyRepository;
  private final SurveyRepository surveyRepository;
  private final PlaceRepository placeRepository;
  private final PlaceSearchRepository placeSearchRepository;
  private final UserScrapPlaceRepository userScrapPlaceRepository;
  private final KeywordRepository keywordRepository;
  private final RecommendationDomainService recommendationDomainService;
  private final RedisUtils redisUtils;
  private final RedisTemplate<String, Object> redisTemplate;

  private static final List<Integer> 키워드_순서에_따른_장소_컨텐츠_선정_개수 = List.of(7, 4, 3, 2, 1);
  private static final List<Integer> 높은_응답_리스트 = List.of(3, 4, 5);
  private static final int 최대_추천_크기 = 5;
  private final PlaceTypeRepository placeTypeRepository;
  private final MatchQueryUseCase matchQueryUseCase;

  public List<String> getMatchReasons(Long user1, Long user2) {
    List<AnswerWeightPair> pairs = 설문_응답쌍과_연관_가중치_조회(user1, user2);
    List<Long> surveyIds = pairs.stream().map(AnswerWeightPair::getSurveyId).distinct().toList();
    Map<Long, Survey> surveyMap = surveyRepository.findAllById(surveyIds).stream()
        .collect(Collectors.toMap(Survey::getId, Function.identity()));

    return pairs.stream()
        .filter(pair -> surveyMap.containsKey(pair.getSurveyId()))
        .map(pair -> {
          Survey survey = surveyMap.get(pair.getSurveyId());
          return 높은_응답_리스트.contains(pair.getAnswer())
              ? survey.getMatchedReasonForHigherAnswer()
              : survey.getMatchedReasonForLowerAnswer();
        })
        .limit(5)
        .toList();
  }

  public List<GetPlaceDetailResponseDto> 매칭된_커플을_위한_장소_컨텐츠_추천(User user1, Long matchedUserId) {
    User user2 = userQueryUseCase.유저_찾기_혹은_오류(matchedUserId);
    matchQueryUseCase.매칭된_유저들인지_검증(user1, user2);
    List<String> keywords = 주요_매칭_키워드_조회(user1.getId(), user2.getId());
    List<Place> places = 키워드_기반_장소_컨텐츠_조회(keywords);
    if (places.size() < 5) places = 추천_장소가_너무_적을경우_링고_추천_장소_조회();
    return 장소_컨텐츠_상세_정보_생성(places, user1);
  }

  @Scheduled(cron = "0 30 0 * * *")
  public void 개인화된_장소_컨텐츠_cache() {
    userQueryUseCase.findAll().forEach(this::개인화된_장소_컨텐츠_응답_dto_생성_및_cache);
  }

  public List<GetPlaceDetailResponseDto> getIndividualUserPlaces(User user) {
    String cacheKey = RedisKey.개인_장소_추천_레디스_키;
    List<GetPlaceDetailResponseDto> cached = 캐시된_장소_컨텐츠_조회(cacheKey, user.getId());
    if (cached != null) return cached;

    List<Place> 인기_컨텐츠 = placeRepository.가장_많이_클릭한_컨텐츠_상위_5개만_조회();
    List<GetPlaceDetailResponseDto> 상세정보 = 장소_컨텐츠_상세_정보_생성(인기_컨텐츠, user);

    장소_컨텐츠_cache(cacheKey, user.getId(), 상세정보);
    return 상세정보;
  }

  public List<GetTypePlaceRequestDto> 주제별_장소_컨텐츠_추천(User 유저){

    List<PlaceTypeEntity> 타입_리스트 = placeTypeRepository.findAll();
    if (타입_리스트.size() < 2) return null;

    PlaceType 타입1 = 타입_리스트.get(0).getType();
    PlaceType 타입2 = 타입_리스트.get(1).getType();

    List<GetPlaceDetailResponseDto> 타입1_장소_정보 = 타입_기반_장소_컨텐츠_추천(타입1.toString(), 유저);
    List<GetPlaceDetailResponseDto> 타입2_장소_정보 = 타입_기반_장소_컨텐츠_추천(타입2.toString(), 유저);

    타입1_장소_정보 = 타입1_장소_정보.subList(0, Math.min(최대_추천_크기, 타입1_장소_정보.size()));
    타입2_장소_정보 = 타입2_장소_정보.subList(0, Math.min(최대_추천_크기, 타입2_장소_정보.size()));

    GetTypePlaceRequestDto dto1 = new GetTypePlaceRequestDto(타입1.getDescription(), 타입1_장소_정보);
    GetTypePlaceRequestDto dto2 = new GetTypePlaceRequestDto(타입2.getDescription(), 타입2_장소_정보);

    return List.of(dto1, dto2);
  }

  public List<GetPlaceDetailResponseDto> 타입_기반_장소_컨텐츠_추천(String 타입, User 유저){
    List<Place> 장소_리스트 = placeRepository.findAllByTypeWithImages(타입)
        .stream()
        .sorted((a, b) -> Math.toIntExact(b.getClickCount() - a.getClickCount()))
        .toList();
    return 장소_컨텐츠_상세_정보_생성(장소_리스트, 유저);
  }

  public List<GetPlaceDetailResponseDto> getRankedPagedPlaces(User user, int page, int size) {
    Page<Long> placeIds = placeRepository.findPlaceIdOrderByClickCount(PageRequest.of(page, size));

    List<Place> places = placeRepository.findPlaceByIdsWithImages(placeIds.stream().toList());
    Map<Long, Place> placeIdMap = places.stream().collect(Collectors.toMap(Place::getId, Function.identity()));
    List<Place> result = placeIds.stream().map(placeIdMap::get).toList();

    return 장소_컨텐츠_상세_정보_생성(result, user);
  }

  public List<String> 주요_매칭_키워드_조회(Long user1, Long user2) {
    List<AnswerWeightPair> 설문_응답_가중치_리스트 = 설문_응답쌍과_연관_가중치_조회(user1, user2);

    List<Long> surveyIds = 설문_응답_가중치_리스트.stream().map(AnswerWeightPair::getSurveyId).distinct().toList();
    Map<Long, Survey> 설문_id_설문_맵 = surveyRepository.findAllById(surveyIds).stream()
        .collect(Collectors.toMap(Survey::getId, Function.identity()));

    Map<String, Long> 키워드_가중치_맵 = 설문_응답_가중치_리스트.stream()
        .filter(pair -> 설문_id_설문_맵.containsKey(pair.getSurveyId()))
        .flatMap(pair -> {
          Survey survey = 설문_id_설문_맵.get(pair.getSurveyId());
          String 설문_응답과_관련된_키워드 = 높은_응답_리스트.contains(pair.getAnswer())
              ? survey.getPositiveKeyword().strip()
              : survey.getNegativeKeyword().strip();
          return 키워드_토큰화(설문_응답과_관련된_키워드).stream()
              .map(s -> Map.entry(s, (long) pair.getRelationWeight()));
        })
        // 동일한 키워드의 가중치는 모두 합한다.
        .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingLong(Map.Entry::getValue)));

    // 키워드에는 고유한 가중치가 존재한다. 키워드_가중치_맵은 설문 응답에 따른 가중치를 계산한 결과라 두 가중치를 구별해야한다.
    Map<String, Integer> 고유_키워드_가중치_맵 = keywordRepository.findAllByKeywordIn(키워드_가중치_맵.keySet())
        .stream()
        .collect(Collectors.toMap(Keyword::getKeyword, Keyword::getScore));

    return 키워드_가중치_맵.entrySet().stream()
        .filter(e -> 고유_키워드_가중치_맵.containsKey(e.getKey()))
        .map(entry -> Map.entry(entry.getKey(), 고유_키워드_가중치_맵.get(entry.getKey()) * entry.getValue()))
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey)
        .limit(5)
        .toList();
  }

  public List<String> 유저_설문_기반_키워드_추출(User user) {
    List<AnsweredSurvey> 유저가_응답한_설문들 = answeredSurveyRepository.findAllByUser(user);

    List<Integer> 유저가_응답한_설문_번호 = 유저가_응답한_설문들.stream().map(AnsweredSurvey::getSurveyNum).distinct().toList();
    Map<Integer, Survey> 설문번호_설문_맵 = surveyRepository.findAllBySurveyNumIn(유저가_응답한_설문_번호).stream()
        .collect(Collectors.toMap(Survey::getSurveyNum, Function.identity()));

    Map<String, Long> 키워드_가중치_맵 = 유저가_응답한_설문들.stream()
        .filter(answeredSurvey -> 설문번호_설문_맵.containsKey(answeredSurvey.getSurveyNum()))
        .flatMap(answeredSurvey -> {
          Survey survey = 설문번호_설문_맵.get(answeredSurvey.getSurveyNum());
          long 응답점수 = recommendationDomainService.응답_점수_계산(answeredSurvey.getAnswer());
          String 응답과_관련된_키워드 = 높은_응답_리스트.contains(answeredSurvey.getAnswer())
              ? survey.getPositiveKeyword()
              : survey.getNegativeKeyword();
          return 키워드_토큰화(응답과_관련된_키워드).stream()
              .map(key -> Map.entry(key, 응답점수));
        })
        .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingLong(Map.Entry::getValue)));

    Map<String, Integer> 키워드_고유_가중치_맵 = keywordRepository.findAllByKeywordIn(키워드_가중치_맵.keySet()).stream()
        .collect(Collectors.toMap(Keyword::getKeyword, Keyword::getScore));

    return 키워드_가중치_맵.entrySet().stream()
        .filter(e -> 키워드_고유_가중치_맵.containsKey(e.getKey()))
        .map(entry -> Map.entry(entry.getKey(), (long) 키워드_고유_가중치_맵.get(entry.getKey()) * entry.getValue()))
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey)
        .limit(5)
        .toList();
  }

  public List<GetPlaceDetailResponseDto> 장소_컨텐츠_상세_정보_생성(List<Place> places, User user) {
    Set<Long> 스크랩된_장소_ids = userScrapPlaceRepository.findAllByUserWithImages(user).stream()
        .map(UserScrapPlace::getPlace)
        .map(Place::getId)
        .collect(Collectors.toSet());
    return places.stream()
        .map(place -> place.createPlaceDetailDto(스크랩된_장소_ids.contains(place.getId())))
        .toList();
  }

  void 개인화된_장소_컨텐츠_응답_dto_생성_및_cache(User user) {
    String key = RedisKey.개인_장소_추천_레디스_키;
    List<String> keywords = 유저_설문_기반_키워드_추출(user);
    List<Place> places = 키워드_기반_장소_컨텐츠_조회(keywords);
    if (places.size() < 5) places = 추천_장소가_너무_적을경우_링고_추천_장소_조회();
    List<GetPlaceDetailResponseDto> result = 장소_컨텐츠_상세_정보_생성(places, user);
    장소_컨텐츠_cache(key, user.getId(), result);
  }

  private List<Place> 키워드_기반_장소_컨텐츠_조회(List<String> keywords) {
    List<Place> result = new ArrayList<>();
    for (int index = 0; index < keywords.size(); index++) {
      List<Place> places = 키워드가_포함된_장소_검색(keywords.get(index));
      int max = Math.min(키워드_순서에_따른_장소_컨텐츠_선정_개수.get(index), places.size());
      Collections.shuffle(places);
      result.addAll(places.subList(0, max));
    }
    Collections.shuffle(result);
    return result;
  }

  private List<Place> 키워드가_포함된_장소_검색(String 키워드){
    List<Long> placeIds = placeSearchRepository.findAllByKeywordContaining(키워드)
        .stream().map(PlaceDocument::getId).toList();
    return placeRepository.findAllByIdInWithImages(placeIds);
  }

  private List<Place> 추천_장소가_너무_적을경우_링고_추천_장소_조회() {
    List<Place> places = placeRepository.findAllByTypeWithImages("RINGO");
    Collections.shuffle(places);
    return places.subList(0, Math.min(places.size(), 10));
  }

  private List<AnswerWeightPair> 설문_응답쌍과_연관_가중치_조회(Long user1, Long user2) {
    return answeredSurveyRepository.두_유저가_응답한_문항중_쌍문항만_조회(user1, user2)
        .stream()
        .filter(pair -> Math.abs(pair.getAnswer() - pair.getConfrontAnswer()) <= 2)
        .map(pair -> {
          int weight = recommendationDomainService.설문_응답쌍_연관_가중치_계산(pair.getAnswer(), pair.getConfrontAnswer());
          return new AnswerWeightPair(pair.getAnswer(), pair.getConfrontAnswer(), pair.getSurveyId(), weight);
        })
        .sorted((a, b) -> b.getRelationWeight() - a.getRelationWeight())
        .toList();
  }

  private List<String> 키워드_토큰화(String keywords) {
    return Arrays.stream(keywords.split(",")).toList();
  }

  @SuppressWarnings("unchecked")
  public List<GetPlaceDetailResponseDto> 캐시된_장소_컨텐츠_조회(String key, Long userId) {
    if (redisTemplate.hasKey(key + userId)) {
      return ((ApiListResponseDto<GetPlaceDetailResponseDto>) redisTemplate.opsForValue().get(key + userId)).getList();
    }
    return new ArrayList<>();
  }

  private void 장소_컨텐츠_cache(String key, Long userId, List<GetPlaceDetailResponseDto> list) {
    redisUtils.cacheUntilMidnight(key + userId, new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), list));
  }

  @Getter
  @RequiredArgsConstructor
  public static class AnswerWeightPair {
    private final int answer;
    private final int confront;
    private final Long surveyId;
    private final int relationWeight;
  }
}
