package com.lingo.lingoproject.security.oauth;


import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.domain.enums.SignupStatus;
import com.lingo.lingoproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthUtils {

  private final UserRepository userRepository;

  public User signup(String loginId){
    log.info("가입되지 않은 회원입니다. 새로 계정을 만듭니다.");
    User user = User.builder()
        .loginId(loginId)
        .role(Role.USER)
        .status(SignupStatus.BEFORE)
        .build();
    User savedUser = userRepository.save(user);
    return savedUser;
  }
  public void login(User user){
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user.getLoginId(), "password", user.getAuthorities())
    );
  }
}
