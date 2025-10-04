package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
  private final UserRepository userRepository;

  public void deleteUser(Long userId){
    userRepository.deleteById(userId);
  }

  public String findUserId(HttpSession session) throws Exception{
    if(session.getAttribute("isAuthenticated").equals("true")){
      String phoneNumber = (String) session.getAttribute("phoneNumber");
      User user = userRepository.findByPhoneNumber(phoneNumber)
          .orElseThrow(() -> new IllegalArgumentException("회원이 아닙니다."));
      return user.getUsername();
    }
    else{
      throw new Exception("인증되지 않았습니다.");
    }
  }

  public void resetPassword(String password, HttpSession session) throws Exception{
    if(session.getAttribute("isAuthenticated").equals("true")){
      String phoneNumber = (String) session.getAttribute("phoneNumber");
      User user = userRepository.findByPhoneNumber(phoneNumber)
          .orElseThrow(() -> new IllegalArgumentException("회원이 아닙니다."));
      user.setPassword(password);
      userRepository.save(user);
    }
    else{
      throw new Exception("인증되지 않았습니다.");
    }
  }
}
