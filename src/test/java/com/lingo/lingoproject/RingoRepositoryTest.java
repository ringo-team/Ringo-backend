package com.lingo.lingoproject;


import com.lingo.lingoproject.shared.domain.elastic.PlaceDocument;
import com.lingo.lingoproject.shared.domain.model.FailedFcmMessageLog;
import com.lingo.lingoproject.shared.domain.model.Place;
import com.lingo.lingoproject.shared.domain.model.PlaceImage;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.elastic.PlaceSearchRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PlaceImageRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PlaceRepository;
import com.lingo.lingoproject.image.application.S3ImageStorageService;
import com.lingo.lingoproject.shared.infrastructure.retry.RedisQueueMessagePayLoad;
import com.lingo.lingoproject.shared.infrastructure.retry.RedisQueueService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@ActiveProfiles("dev")
public class RingoRepositoryTest {

  @Autowired
  S3ImageStorageService imageService;

  @Autowired
  RedisQueueService redisQueueService;
  @Autowired
  private PlaceRepository placeRepository;
  @Autowired
  private PlaceSearchRepository placeSearchRepository;
  @Autowired
  private PlaceImageRepository placeImageRepository;
  @Autowired
  private RestTemplate restTemplate;
  @Autowired
  private S3ImageStorageService s3ImageStorageService;

  @Test
  public void updatePlaceImage(){
    List<PlaceImage> images = placeImageRepository.findAll();
    LocalDate today = LocalDate.now();
    String path = "/place/" + today.getYear() + "/" +
        today.getYear() + "-" + today.getMonthValue() + "/" + today + "/";
    for (PlaceImage image : images) {
      String url = getUrl(image, path);
      image.setImage(url);
    }
    placeImageRepository.saveAll(images);
  }

  public String getUrl(PlaceImage image, String path){
    try {
      byte[] imageByte = restTemplate.getForObject(image.getImage(), byte[].class);
      String filename = path + UUID.randomUUID() + "_image.jpg";
      s3ImageStorageService.이미지_byte를_S3에_업로드(filename, "image/jpeg", imageByte);
      return s3ImageStorageService.이미지_url_생성(filename);
    } catch (Exception e) {
      return null;
    }
  }


  @Test
  public void getSuspendedUserTest(){
    List<PlaceDocument> placeDocuments = placeRepository.findAll()
        .stream()
        .map(Place::createDocument)
        .toList();
    placeSearchRepository.saveAll(placeDocuments);
  }

  @Test
  public void getSubstringS3ImageUrl(){
    String url = "http://localhost:8080/amazonaws.com/2025/2025-12/2025-12-25/profile";
    Assertions.assertEquals("2025/2025-12/2025-12-25/profile", imageService.S3_이미지_키_추출(url));
  }

  @Test
  public void pushEntityToRedisQueue(){
    RedisQueueMessagePayLoad payload = FailedFcmMessageLog.of(
        new RingoException("에러", ErrorCode.INTERNAL_SERVER_ERROR),
        "token",
        "title",
        "message"
        );
    redisQueueService.pushToQueue("FCM", payload);
  }
}
