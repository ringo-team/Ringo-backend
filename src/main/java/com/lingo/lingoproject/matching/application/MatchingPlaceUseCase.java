package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.community.presentation.dto.GetPlaceDetailResponseDto;
import com.lingo.lingoproject.matching.domain.service.RecommendationDomainService;
import com.lingo.lingoproject.shared.domain.elastic.PlaceDocument;
import com.lingo.lingoproject.shared.domain.model.AnsweredSurvey;
import com.lingo.lingoproject.shared.domain.model.Keyword;
import com.lingo.lingoproject.shared.domain.model.Place;
import com.lingo.lingoproject.shared.domain.model.Survey;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.infrastructure.elastic.PlaceSearchRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.AnsweredSurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.KeywordRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PlaceRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.SurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserScrapPlaceRepository;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.exception.ErrorCode;
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

  private static final List<Integer> PLACE_SELECTION_COUNT = List.of(7, 4, 3, 2, 1);
  private static final List<Integer> POSITIVE_ANSWER_LIST = List.of(3, 4, 5);

  public List<String> getMatchReasons(Long user1, Long user2) {
    List<SortedAnswerPairWithWeight> pairs = findSortedRelatedAnswerPairs(user1, user2);
    List<Long> surveyIds = pairs.stream().map(SortedAnswerPairWithWeight::getSurveyId).distinct().toList();
    Map<Long, Survey> surveyMap = surveyRepository.findAllById(surveyIds).stream()
        .collect(Collectors.toMap(Survey::getId, Function.identity()));

    return pairs.stream()
        .filter(pair -> surveyMap.containsKey(pair.getSurveyId()))
        .map(pair -> {
          Survey survey = surveyMap.get(pair.getSurveyId());
          return POSITIVE_ANSWER_LIST.contains(pair.getAnswer())
              ? survey.getMatchedReasonForHigherAnswer()
              : survey.getMatchedReasonForLowerAnswer();
        })
        .limit(5)
        .toList();
  }

  public List<GetPlaceDetailResponseDto> getMatchedUserPlaces(User user, Long user1, Long user2) {
    List<String> keywords = getMatchedKeywords(user1, user2);
    List<Place> places = selectPlacesByKeywords(keywords);
    if (places.size() < 4) places = dealWithHasFewKeywords();
    return buildPlaceDetailInfo(places, user);
  }

  @Scheduled(cron = "0 30 23 * * *")
  public void cacheIndividualUserPlaces() {
    userQueryUseCase.findAll().forEach(this::buildPlaceDtoAndCache);
  }

  public List<GetPlaceDetailResponseDto> getIndividualUserPlaces(User user) {
    String key = "individual-place::";
    List<GetPlaceDetailResponseDto> cached = getCachedUserPlaces(key, user.getId());
    if (cached != null) return cached;

    List<Place> places = placeRepository.getPlaceOrderByClickCountLimitFive();
    List<GetPlaceDetailResponseDto> result = buildPlaceDetailInfo(places, user);

    cacheUserPlace(key, user.getId(), result);
    return result;
  }

  public List<GetPlaceDetailResponseDto> getRandomlySelectedPlaces(User user) {
    String key = "random-place::";
    List<GetPlaceDetailResponseDto> cached = getCachedUserPlaces(key, user.getId());
    if (cached != null) return cached;

    List<Place> places = placeRepository.findAllByTypeNotNull();
    Collections.shuffle(places);
    List<GetPlaceDetailResponseDto> result = buildPlaceDetailInfo(
        places.subList(0, Math.min(places.size(), 50)), user
    );
    cacheUserPlace(key, user.getId(), result);
    return result;
  }

  public List<GetPlaceDetailResponseDto> getRankedPagedPlaces(User user, int page, int size) {
    List<Place> places = placeRepository.findAll(PageRequest.of(page, size)).getContent();
    return buildPlaceDetailInfo(places, user);
  }

  public List<String> getMatchedKeywords(Long user1, Long user2) {
    List<SortedAnswerPairWithWeight> pairs = findSortedRelatedAnswerPairs(user1, user2);

    List<Long> surveyIds = pairs.stream().map(SortedAnswerPairWithWeight::getSurveyId).distinct().toList();
    Map<Long, Survey> surveyMap = surveyRepository.findAllById(surveyIds).stream()
        .collect(Collectors.toMap(Survey::getId, Function.identity()));

    Map<String, Long> keywordWeightMap = pairs.stream()
        .filter(pair -> surveyMap.containsKey(pair.getSurveyId()))
        .flatMap(pair -> {
          Survey survey = surveyMap.get(pair.getSurveyId());
          String keyword = POSITIVE_ANSWER_LIST.contains(pair.getAnswer())
              ? survey.getKeywordForHigherAnswer().strip()
              : survey.getKeywordForLowerAnswer().strip();
          return tokenizeKeywords(keyword).stream()
              .map(s -> Map.entry(s, (long) pair.getOrderWeight()));
        })
        .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingLong(Map.Entry::getValue)));

    Map<String, Integer> keywordScoreMap = keywordRepository.findAllByKeywordIn(keywordWeightMap.keySet()).stream()
        .collect(Collectors.toMap(Keyword::getKeyword, Keyword::getScore));

    return keywordWeightMap.entrySet().stream()
        .filter(e -> keywordScoreMap.containsKey(e.getKey()))
        .map(entry -> Map.entry(entry.getKey(), keywordScoreMap.get(entry.getKey()) * entry.getValue()))
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey)
        .limit(5)
        .toList();
  }

  public List<String> getIndividualSurveyBasedKeywords(User user) {
    List<AnsweredSurvey> answeredSurveys = answeredSurveyRepository.findAllByUser(user);

    List<Integer> surveyNums = answeredSurveys.stream().map(AnsweredSurvey::getSurveyNum).distinct().toList();
    Map<Integer, Survey> surveyMap = surveyRepository.findAllBySurveyNumIn(surveyNums).stream()
        .collect(Collectors.toMap(Survey::getSurveyNum, Function.identity()));

    Map<String, Long> keywordWeights = answeredSurveys.stream()
        .filter(answeredSurvey -> surveyMap.containsKey(answeredSurvey.getSurveyNum()))
        .flatMap(answeredSurvey -> {
          Survey survey = surveyMap.get(answeredSurvey.getSurveyNum());
          long answerScore = recommendationDomainService.calculateAnswerScore(answeredSurvey.getAnswer());
          String keyword = POSITIVE_ANSWER_LIST.contains(answeredSurvey.getAnswer())
              ? survey.getKeywordForHigherAnswer()
              : survey.getKeywordForLowerAnswer();
          return tokenizeKeywords(keyword).stream()
              .map(key -> Map.entry(key, answerScore));
        })
        .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingLong(Map.Entry::getValue)));

    Map<String, Integer> keywordScoreMap = keywordRepository.findAllByKeywordIn(keywordWeights.keySet()).stream()
        .collect(Collectors.toMap(Keyword::getKeyword, Keyword::getScore));

    return keywordWeights.entrySet().stream()
        .filter(e -> keywordScoreMap.containsKey(e.getKey()))
        .map(entry -> Map.entry(entry.getKey(), (long) keywordScoreMap.get(entry.getKey()) * entry.getValue()))
        .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
        .map(Map.Entry::getKey)
        .limit(5)
        .toList();
  }

  public List<GetPlaceDetailResponseDto> buildPlaceDetailInfo(List<Place> places, User user) {
    Set<Long> scrappedPlaceIds = userScrapPlaceRepository.findAllByUser(user).stream()
        .map(scrap -> scrap.getPlace().getId())
        .collect(Collectors.toSet());
    return places.stream()
        .map(place -> place.createPlaceDetailDto(scrappedPlaceIds.contains(place.getId())))
        .toList();
  }

  private void buildPlaceDtoAndCache(User user) {
    String key = "individual-place::";
    List<String> keywords = getIndividualSurveyBasedKeywords(user);
    List<Place> places = selectPlacesByKeywords(keywords);
    if (places.size() < 4) places = dealWithHasFewKeywords();
    List<GetPlaceDetailResponseDto> result = buildPlaceDetailInfo(places, user);
    cacheUserPlace(key, user.getId(), result);
  }

  private List<Place> selectPlacesByKeywords(List<String> keywords) {
    List<Place> result = new ArrayList<>();
    for (int index = 0; index < keywords.size(); index++) {
      List<Long> placeIds = placeSearchRepository.findAllByKeywordContaining(keywords.get(index))
          .stream().map(PlaceDocument::getId).toList();
      List<Place> places = placeRepository.findAllByIdIn(placeIds);
      int max = Math.min(PLACE_SELECTION_COUNT.get(index), places.size());
      Collections.shuffle(places);
      result.addAll(places.subList(0, max));
    }
    Collections.shuffle(result);
    return result;
  }

  private List<Place> dealWithHasFewKeywords() {
    List<Place> places = placeRepository.findAllByType("RINGO_PICK");
    Collections.shuffle(places);
    return places.subList(0, Math.min(places.size(), 10));
  }

  private List<SortedAnswerPairWithWeight> findSortedRelatedAnswerPairs(Long user1, Long user2) {
    return answeredSurveyRepository.getRelatedSurveyAnswerPairs(user1, user2)
        .stream()
        .filter(pair -> Math.abs(pair.getAnswer() - pair.getConfrontAnswer()) <= 2)
        .sorted((a, b) -> {
          int scoreA = recommendationDomainService.calculateAnswerPairScore(a.getAnswer(), a.getConfrontAnswer());
          int scoreB = recommendationDomainService.calculateAnswerPairScore(b.getAnswer(), a.getConfrontAnswer());
          return scoreB - scoreA;
        })
        .map(pair -> {
          int weight = recommendationDomainService.calculateAnswerPairScore(pair.getAnswer(), pair.getConfrontAnswer());
          return new SortedAnswerPairWithWeight(pair.getAnswer(), pair.getConfrontAnswer(), pair.getSurveyId(), weight);
        })
        .toList();
  }

  private List<String> tokenizeKeywords(String keywords) {
    return Arrays.stream(keywords.split(",")).toList();
  }

  @SuppressWarnings("unchecked")
  private List<GetPlaceDetailResponseDto> getCachedUserPlaces(String key, Long userId) {
    if (redisTemplate.hasKey(key + userId)) {
      return ((ApiListResponseDto<GetPlaceDetailResponseDto>) redisTemplate.opsForValue().get(key + userId)).getList();
    }
    return null;
  }

  private void cacheUserPlace(String key, Long userId, List<GetPlaceDetailResponseDto> list) {
    redisUtils.cacheUntilMidnight(key + userId, new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), list));
  }

  @Getter
  @RequiredArgsConstructor
  public static class SortedAnswerPairWithWeight {
    private final int answer;
    private final int confront;
    private final Long surveyId;
    private final int orderWeight;
  }
}
