package com.lingo.lingoproject;


import com.lingo.lingoproject.match.MatchService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
public class RingoRepositoryTest {

  @Autowired
  MatchService matchService;

  @Test
  public void getSuspendedUserTest(){
    List<Long> banIds = matchService.getExcludedUserIdsForRecommendation(5L);
    System.out.println("-----밴당한 유저 아이디 : " + banIds);
  }
}
