package com.lingo.lingoproject.security.oauth.google;


import com.lingo.lingoproject.domain.OAuthToken;
import com.lingo.lingoproject.repository.OAuthTokenRepository;
import com.lingo.lingoproject.security.oauth.OAuthUtils;
import com.lingo.lingoproject.security.oauth.google.dto.GoogleTokenResponseDto;
import com.lingo.lingoproject.security.oauth.google.dto.GoogleUserInfoResponseDto;
import com.lingo.lingoproject.security.oauth.kakao.dto.KakaoUserInfoResponseDto;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class GoogleLoginService {

  private final String GOOGLE_TOKEN_URL = "https://oauth2.googleapis.com/token";
  private final String GOOGLE_GMAIL_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

  @Value("${oauth.google.client_id}")
  private String clientId;

  @Value("${oauth.google.client_secret}")
  private String clientSecret;

  @Value("${oauth.google.redirect_uri}")
  private String redirectUri;

  private final RestTemplate restTemplate;
  private final OAuthTokenRepository oAuthTokenRepository;
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
      throw new RestClientException(e.getMessage());
    }
    if (response == null){
      throw new RestClientException("Invalid Google Access Token");
    }
    return response.accessToken();
  }

  public void saveUserLoginInfo(String code){
    String token = getGoogleAccessToken(code);
    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(headers);
    GoogleUserInfoResponseDto response = null;
    try{
      response = restTemplate.exchange(GOOGLE_GMAIL_URL, HttpMethod.GET,
          request, GoogleUserInfoResponseDto.class).getBody();
    }catch (Exception e){
      throw new RestClientException(e.getMessage());
    }
    if(response == null){
      throw new RestClientException("Kakao user info response is null");
    }
    Optional<OAuthToken> oAuthToken = oAuthTokenRepository.findByUserToken(response.email());
    if(oAuthToken.isEmpty()){
      oAuthUtils.signup(response.email());
    }
    oAuthUtils.login(oAuthToken.get());
  }
}
