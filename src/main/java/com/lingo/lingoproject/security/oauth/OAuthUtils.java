package com.lingo.lingoproject.security.oauth;


import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.domain.enums.SignupStatus;
import com.lingo.lingoproject.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuthUtils {

  private final UserRepository userRepository;

  public User signup(String email){
    log.info("가입되지 않은 회원입니다. 새로 계정을 만듭니다.");
    User user = User.builder()
        .email(email)
        .role(Role.USER)
        .status(SignupStatus.BEFORE)
        .build();
    User savedUser = userRepository.save(user);
    return savedUser;
  }
  public void login(User user){
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user.getEmail(), "password", user.getAuthorities())
    );
  }
}
