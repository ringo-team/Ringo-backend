package com.lingo.lingoproject.user.infrastructure.security;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class CustomUserDetailService implements UserDetailsService {

  private final UserQueryUseCase userQueryUseCase;

  @Override
  public UserDetails loadUserByUsername(String username){
    Optional<User> user = userQueryUseCase.findByLoginId(username);
    if (user.isEmpty()) {
      throw new RingoException(username+"는 가입되지 않은 이메일 입니다.", ErrorCode.FORBIDDEN);
    }
    return user.get();
  }
}
