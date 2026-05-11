package com.lingo.lingoproject.user.application;

import com.google.common.net.HttpHeaders;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class KakaoAuthService {

  private final UserQueryUseCase userQueryUseCase;
  @Value("${oauth.kakao.api}")
  private String kakaoAuthUrl;

  public KakaoAuthService(UserQueryUseCase userQueryUseCase) {
    this.userQueryUseCase = userQueryUseCase;
  }

  public User login(String token){
    WebClient webClient = WebClient.create();

    KakaoValidationRequest response = webClient.get()
        .uri(kakaoAuthUrl)
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
        .retrieve()
        .onStatus(
            HttpStatusCode::is4xxClientError,
            res -> Mono.error(new RingoException("유효하지 않은 Kakao 토큰입니다.", ErrorCode.BAD_REQUEST))
        )
        .bodyToMono(KakaoValidationRequest.class)
        .timeout(Duration.ofSeconds(5))
        .block();
    User user = userQueryUseCase.findByLoginId(response.email()).orElse(null);

    if (user != null) return user;

    User user1 = User.builder()
        .loginId(response.email())
        .password(String.valueOf(token.hashCode()))
        .build();

    User savedUser = userQueryUseCase.save(user1);

    return savedUser;

  }

}
