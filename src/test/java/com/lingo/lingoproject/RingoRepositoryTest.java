package com.lingo.lingoproject;


import com.lingo.lingoproject.domain.FailedFcmMessageLog;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.image.ImageService;
import com.lingo.lingoproject.match.MatchService;
import com.lingo.lingoproject.retry.RedisQueueMessagePayLoad;
import com.lingo.lingoproject.retry.RedisQueueService;
import java.util.List;
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
  ImageService imageService;

  @Autowired
  RedisQueueService redisQueueService;

  @Test
  public void getSuspendedUserTest(){
    List<Long> banIds = matchService.getExcludedUserIdsForRecommendation(5L);
    System.out.println("-----밴당한 유저 아이디 : " + banIds);
  }

  @Test
  public void getSubstringS3ImageUrl(){
    String url = "http://localhost:8080/amazonaws.com/2025/2025-12/2025-12-25/profile";
    Assertions.assertEquals("2025/2025-12/2025-12-25/profile", imageService.getFilenameFromS3ImageUrl(url));
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
