package com.lingo.lingoproject;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.domain.enums.SignupStatus;
import com.lingo.lingoproject.match.dto.MatchScoreResultInterface;
import com.lingo.lingoproject.mongo_repository.MessageRepository;
import com.lingo.lingoproject.repository.AnsweredSurveyRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.services.LoginService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
public class RingoRepositoryTest {
  @Autowired
  AnsweredSurveyRepository answeredSurveyRepository;
  @Autowired
  LoginService loginService;
  @Autowired
  MessageRepository messageRepository;
  @Autowired
  private UserRepository userRepository;


  @Test
  void matchScoreTest(){
    List<MatchScoreResultInterface> scores = answeredSurveyRepository.calcMatchScore(1L, 2L);
    System.out.println("영역별 점수 " + scores.get(0).getCategory() + " : " + scores.get(0).getAvgAnswer());
  }

  @Test
  void generateFriendRecommendationCodeTest(){
    System.out.println(loginService.generateFriendInvitationCode());
  }

  @Test
  void testMongoRepository(){
    int count = messageRepository.findNumberOfNotReadMessages(4L, 3L);
    System.out.println(count);
  }

  @Test
  void generateUsers(){
    List<User> users = new ArrayList<>();
    for(int i = 0; i < 40; i++) {
      User user = User.builder().email("user" + i).password("1234").gender(Gender.FEMALE).role(Role.USER).status(
          SignupStatus.COMPLETED).build();
      users.add(user);
    }
    userRepository.saveAll(users);
  }

  @Test
  void modifyUsers(){
    List<User> users = userRepository.findAll();
    for (User user : users){
      user.set(user.getEmail());
    }
    userRepository.saveAll(users);
  }
}
