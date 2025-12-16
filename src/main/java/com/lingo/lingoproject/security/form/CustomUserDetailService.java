package com.lingo.lingoproject.security.form;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.UserRepository;
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

  private final UserRepository userRepository;

  @Override
  public UserDetails loadUserByUsername(String username){
    Optional<User> user = userRepository.findByLoginId(username);
    if (user.isEmpty()) {
      throw new RingoException(username+"는 가입되지 않은 이메일 입니다.", ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN);
    }
    return user.get();
  }
}
