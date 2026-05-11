package com.lingo.lingoproject.user.application;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

  private final UserQueryUseCase userQueryUseCase;
  @Value("${spring.security.oauth2.client.registration.google.client-id}")
  private String clientId;

  public GoogleAuthService(UserQueryUseCase userQueryUseCase) {
    this.userQueryUseCase = userQueryUseCase;
  }

  public User login(String idToken){
    try {
      GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
          new NetHttpTransport(),
          GsonFactory.getDefaultInstance()
      )
          .setAudience(Collections.singletonList(clientId))
          .build();

      GoogleIdToken googleIdToken = verifier.verify(idToken);
      if (googleIdToken == null) {
        throw new RingoException("유효하지 않은 Google 토큰입니다.", ErrorCode.BAD_REQUEST);
      }

      GoogleIdToken.Payload payload = googleIdToken.getPayload();

      User user = userQueryUseCase.findByLoginId(payload.getEmail()).orElse(null);

      if (user != null) return user;

      User user1 = User.builder()
          .loginId(payload.getEmail())
          .password(payload.getAccessTokenHash())
          .build();

      User savedUser = userQueryUseCase.save(user1);

      return savedUser;

    } catch (GeneralSecurityException | IOException e) {
      throw new RingoException("Google 토큰 검증 실패: " + e.getMessage(), ErrorCode.BAD_REQUEST);
    }
  }
}
