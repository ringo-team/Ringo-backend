package com.lingo.lingoproject;


import com.lingo.lingoproject.image.ImageService;
import com.lingo.lingoproject.match.MatchService;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
public class RingoRepositoryTest {

  @Autowired
  MatchService matchService;

  @Autowired
  ImageService imageService;

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
}
