package com.lingo.lingoproject.fcm;

import com.lingo.lingoproject.domain.FcmToken;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.fcm.dto.RefreshFcmTokenRequestDto;
import com.lingo.lingoproject.repository.FcmTokenRepository;
import com.lingo.lingoproject.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FcmService {

  private final FcmTokenRepository fcmTokenRepository;
  private final UserRepository userRepository;

  public void refreshFcmToken(RefreshFcmTokenRequestDto dto){
    User user = userRepository.findById(dto.userId())
        .orElseThrow(() -> new RingoException("유저를 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    FcmToken fcmToken = fcmTokenRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("토큰을 찾을 수 없습니다.", HttpStatus.INTERNAL_SERVER_ERROR));
    fcmToken.setToken(dto.token());
    fcmTokenRepository.save(fcmToken);
  }

}
