package com.lingo.lingoproject.security.oauth.google;


import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.oauth.OAuthUtils;
import com.lingo.lingoproject.security.oauth.google.dto.GoogleTokenResponseDto;
import com.lingo.lingoproject.security.oauth.google.dto.GoogleUserInfoResponseDto;
import jakarta.transaction.Transactional;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GoogleLoginService {

  private final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
  private final String GOOGLE_GMAIL_URL = "https://www.googleapis.com/oauth2/v2/userinfo";
  private final UserRepository userRepository;

  @Value("${oauth.google.client_id}")
  private String clientId;

  @Value("${oauth.google.client_secret}")
  private String clientSecret;

  @Value("${oauth.google.redirect_uri}")
  private String redirectUri;

  private final RestTemplate restTemplate;
  private final OAuthUtils oAuthUtils;

  public String getGoogleAccessToken(String code){
    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    params.add("code", code);
    params.add("client_id", clientId);
    params.add("client_secret", clientSecret);
    params.add("redirect_uri", redirectUri);
    params.add("grant_type", "authorization_code");
    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params, headers);
    GoogleTokenResponseDto response = null;
    try{
      response = restTemplate.exchange(GOOGLE_TOKEN_URL, HttpMethod.POST, request, GoogleTokenResponseDto.class).getBody();
    }catch (Exception e){
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (response == null){
      throw new RingoException("Invalid Google Access Token", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return response.accessToken();
  }
  /**
   * 1. code를 이용하여 token을 발급한다.
   * 2. token을 통해 유저 고유 정보를 얻는다. 여기서는 유저의 email 정보를 얻는다.
   * 3. 유저의 email 정보를 통해서 이전 가입여부를 확인한다. 가입한 적이 없으면 자동 회원가입이 진행된다.
   */
  @Transactional
  public User saveUserLoginInfo(String code){
    //1
    String token = getGoogleAccessToken(code);

    //2
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(headers);
    GoogleUserInfoResponseDto response = null;
    try{
      response = restTemplate.exchange(GOOGLE_GMAIL_URL, HttpMethod.GET,
          request, GoogleUserInfoResponseDto.class).getBody();
    }catch (Exception e){
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if(response == null){
      throw new RingoException("Google user info response is null", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // 3
    Optional<User> user = userRepository.findByEmail(response.email());
    User loginUser = null;

    if(user.isEmpty()){
      loginUser = oAuthUtils.signup(response.email());
    }else{
      loginUser = user.get();
    }
    // 로그인을 진행한다.
    oAuthUtils.login(loginUser);
    return loginUser;
  }
}
