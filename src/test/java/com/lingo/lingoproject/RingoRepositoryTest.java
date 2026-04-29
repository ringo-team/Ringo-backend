package com.lingo.lingoproject;


import com.lingo.lingoproject.shared.domain.elastic.PlaceDocument;
import com.lingo.lingoproject.shared.domain.model.FailedFcmMessageLog;
import com.lingo.lingoproject.shared.domain.model.Place;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.elastic.PlaceSearchRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PlaceRepository;
import com.lingo.lingoproject.shared.infrastructure.storage.S3ImageStorageService;
import com.lingo.lingoproject.matching.application.MatchService;
import com.lingo.lingoproject.shared.infrastructure.retry.RedisQueueMessagePayLoad;
import com.lingo.lingoproject.shared.infrastructure.retry.RedisQueueService;
import jakarta.transaction.Transactional;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
public class RingoRepositoryTest {

  @Autowired
  MatchService matchService;

  @Autowired
  S3ImageStorageService imageService;

  @Autowired
  RedisQueueService redisQueueService;
  @Autowired
  private PlaceRepository placeRepository;
  @Autowired
  private PlaceSearchRepository placeSearchRepository;


  @Test
  public void getSuspendedUserTest(){
    Map<Long, Place> places = placeRepository.findAll()
        .stream()
        .collect(Collectors.toMap(Place::getId, Function.identity()));
    Iterable<PlaceDocument> documents = placeSearchRepository.findAll();
    documents.forEach(d -> {
      String keyword = places.get(d.getId()) != null ?
          places.get(d.getId()).getKeyword() : null;
      if (keyword == null) return;
      d.setKeyword(keyword);
    });
    placeSearchRepository.saveAll(documents);
  }

  @Test
  public void getSubstringS3ImageUrl(){
    String url = "http://localhost:8080/amazonaws.com/2025/2025-12/2025-12-25/profile";
    Assertions.assertEquals("2025/2025-12/2025-12-25/profile", imageService.extractS3ObjectKey(url));
  }

  @Test
  public void pushEntityToRedisQueue(){
    RedisQueueMessagePayLoad payload = FailedFcmMessageLog.of(
        new RingoException("에러", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR),
        "token",
        "title",
        "message"
        );
    redisQueueService.pushToQueue("FCM", payload);
  }
}
