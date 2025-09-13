package com.lingo.lingoproject.security.oauth;

import com.lingo.lingoproject.domain.OAuthToken;
import com.lingo.lingoproject.domain.UserEntity;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.repository.OAuthTokenRepository;
import com.lingo.lingoproject.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
  private final OAuthTokenRepository oAuthTokenRepository;

  public void signup(String userToken){
    log.info("가입되지 않은 회원입니다. 새로 계정을 만듭니다.");
    String email = UUID.randomUUID().toString();
    UserEntity user = UserEntity.builder()
        .email(email)
        .role(Role.USER)
        .build();
    userRepository.save(user);
    OAuthToken oAuthToken = OAuthToken.builder()
        .user(user)
        .userToken(userToken)
        .build();
    oAuthTokenRepository.save(oAuthToken);
  }
  public void login(OAuthToken oAuthToken){
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    UserEntity user = oAuthToken.getUser();
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user.getEmail(), "passwword", authorities)
    );
  }
}
