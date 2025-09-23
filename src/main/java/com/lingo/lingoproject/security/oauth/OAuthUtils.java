package com.lingo.lingoproject.security.oauth;


import com.lingo.lingoproject.domain.UserEntity;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.domain.enums.SignUpStatus;
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

  public UserEntity signup(String email){
    log.info("가입되지 않은 회원입니다. 새로 계정을 만듭니다.");
    String id = UUID.randomUUID().toString();
    UserEntity user = UserEntity.builder()
        .id(id)
        .email(email)
        .role(Role.USER)
        .status(SignUpStatus.BEFORE)
        .build();
    UserEntity savedUser = userRepository.save(user);
    return savedUser;
  }
  public void login(UserEntity user){
    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
    System.out.println("유저가 로그인 되었습니다.");
    SecurityContextHolder.getContext().setAuthentication(
        new UsernamePasswordAuthenticationToken(user.getEmail(), "password", authorities)
    );
  }
}
