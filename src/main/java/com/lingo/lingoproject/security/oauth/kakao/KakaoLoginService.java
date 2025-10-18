package com.lingo.lingoproject.security.oauth.kakao;



import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.security.oauth.OAuthUtils;
import com.lingo.lingoproject.security.oauth.kakao.dto.KakaoTokenResponseDto;
import com.lingo.lingoproject.security.oauth.kakao.dto.KakaoUserInfoResponseDto;
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
public class KakaoLoginService {

  private final String KAKAO_TOKEN_URL = "https://kauth.kakao.com/oauth/token";
  private final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

  @Value("${oauth.kakao.client_id}")
  private String clientId;

  @Value("${oauth.kakao.redirect_uri}")
  private String redirectUri;

  private final RestTemplate restTemplate;
  private final OAuthUtils oAuthUtils;
  private final UserRepository userRepository;

  public String getKakaoAccessToken(String code){
    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    params.add("grant_type", "authorization_code");
    params.add("client_id", clientId);
    params.add("redirect_uri", redirectUri);
    params.add("code", code);
    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params, headers);
    KakaoTokenResponseDto response = null;
    try {
      response = restTemplate.exchange(KAKAO_TOKEN_URL, HttpMethod.POST,
          request, KakaoTokenResponseDto.class).getBody();
    }catch (Exception e){
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if(response == null){
      throw new RingoException("Kakao token response is null", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return response.accessToken();
  }

  public User saveUserLoginInfo(String code){
    String token = getKakaoAccessToken(code);
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(headers);
    KakaoUserInfoResponseDto response = null;
    try{
      response = restTemplate.exchange(KAKAO_USER_INFO_URL, HttpMethod.GET,
          request, KakaoUserInfoResponseDto.class).getBody();
    }catch (Exception e){
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if(response == null){
      throw new RingoException("Kakao user info response is null", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    Optional<User> user = userRepository.findByEmail(response.id().toString());
    User loginUser = null;
    if(user.isEmpty()){
      loginUser = oAuthUtils.signup(response.id().toString());
    }
    else{
      loginUser = user.get();
    }
    oAuthUtils.login(loginUser);
    return loginUser;
  }
}
