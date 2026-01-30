package com.lingo.lingoproject.security.oauth.kakao;



import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.oauth.OAuthUtils;
import com.lingo.lingoproject.security.oauth.kakao.dto.KakaoTokenResponseDto;
import com.lingo.lingoproject.security.oauth.kakao.dto.KakaoUserInfoResponseDto;
import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class KakaoLoginService {

  private final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
  private final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

  @Value("${oauth.kakao.client_id}")
  private String clientId;

  @Value("${oauth.kakao.redirect_uri}")
  private String redirectUri;

  private final OAuthUtils oAuthUtils;
  private final UserRepository userRepository;

  public String getKakaoAccessToken(String code){
    WebClient webClient = WebClient.create();
    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();

    params.add("grant_type", "authorization_code");
    params.add("client_id", clientId);
    params.add("redirect_uri", redirectUri);
    params.add("code", code);

    KakaoTokenResponseDto response;
    try {
      response = webClient
          .post()
          .uri(KAKAO_TOKEN_URL)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .body(BodyInserters.fromValue(params))
          .retrieve()
          .bodyToMono(KakaoTokenResponseDto.class)
          .timeout(Duration.ofSeconds(5))
          .block();
    }catch (Exception e){
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if(response == null){
      throw new RingoException("Kakao token response is null", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return response.accessToken();
  }

  public User saveUserLoginInfo(String code){

    WebClient webClient = WebClient.create();

    String token = getKakaoAccessToken(code);

    KakaoUserInfoResponseDto response;

    try{
      response = webClient
          .get()
          .uri(KAKAO_USER_INFO_URL)
          .headers(httpHeaders -> {
            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            httpHeaders.set(HttpHeaders.AUTHORIZATION, "Bearer " + token);
          })
          .retrieve()
          .bodyToMono(KakaoUserInfoResponseDto.class)
          .timeout(Duration.ofSeconds(5))
          .block();
    }catch (Exception e){
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if(response == null){
      throw new RingoException("Kakao user info response is null", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    Optional<User> user = userRepository.findByLoginId(response.id().toString());
    User loginUser = user.orElseGet(() -> oAuthUtils.signup(response.id().toString()));

    oAuthUtils.login(loginUser);

    return loginUser;
  }
}
