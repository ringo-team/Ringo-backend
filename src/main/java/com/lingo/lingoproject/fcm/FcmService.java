package com.lingo.lingoproject.fcm;

import com.lingo.lingoproject.domain.FcmToken;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.fcm.dto.SaveFcmTokenRequestDto;
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

  public void refreshFcmToken(SaveFcmTokenRequestDto dto, User user){
    FcmToken fcmToken = fcmTokenRepository.findByUser(user)
        .orElseThrow(() -> new RingoException("유저의 fcm 토큰 엔티티를 찾을 수 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR));
    fcmToken.setToken(dto.token());
    fcmTokenRepository.save(fcmToken);
  }

}
