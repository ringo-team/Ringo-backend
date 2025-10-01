package com.lingo.lingoproject.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfAuthService {

  private final RestTemplate restTemplate;
  private final ObjectMapper objectMapper;

  private String siteCode = null;
  private String tokenVersionId = null;

  @Value("${self-auth.url}")
  private String apiUrl;

  @Value("${self-auth.client_id}")
  private String clientId;

  @Value("${self-auth.client_secret}")
  private String clientSecret;

  @Value("${self-auth.return_url}")
  private String returnUrl;

  @Value("${self-auth.product_id}")
  private String productId;

  public String getAccessToken(){
    apiUrl += "/digital/niceid/oauth/oauth/token";
    String auth = "Basic " + Base64.getEncoder().encodeToString(clientId.getBytes());

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.add("Authorization", auth);

    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "client_credentials");
    params.add("scope", "default");
    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(params, headers);

    GetAccessTokenResponseDto response = null;
    try{
      response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, GetAccessTokenResponseDto.class).getBody();
    } catch (Exception e) {
      throw new RestClientException(e.getMessage());
    }
    if (response == null) {
      throw new RestClientException("No response from api");
    }

    return response.getBody().accessToken();
  }

  public String getCryptoToken(String accessToken){
    apiUrl += "digital/niceid/api/v1.0/common/crypto/token";

    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
    String auth = "bearer " + accessToken +":" + timestamp + ":" + clientId;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.add("Authorization", auth);
    headers.add("ProductId", productId);

    String trId = UUID.randomUUID().toString();
    String dateTime = sdf.format(timestamp);
    GetCryptoTokenRequestDto requestDto = GetCryptoTokenRequestDto
        .builder()
        .header(
            GetCryptoTokenRequestDto
                .dataHeader
                .builder()
                .lang("ko")
                .build()
        )
        .body(
            GetCryptoTokenRequestDto
                .dataBody
                .builder()
                .requestDateTime(dateTime)
                .requestNum(trId)
                .encryptMode("1")
                .build()
        )
        .build();
    HttpEntity<GetCryptoTokenRequestDto> request = new HttpEntity<>(requestDto, headers);

    GetCryptoTokenResponseDto response = null;
    try{
      response = restTemplate.exchange(apiUrl, HttpMethod.POST, request, GetCryptoTokenResponseDto.class).getBody();
    }catch (Exception e){
      throw new RestClientException(e.getMessage());
    }
    if(response == null
        || !response.getHeader().resultCd().equals("1200")
        || !response.getBody().responseCD().equals("P000")
        || response.getBody().responseMsg().startsWith("EAPI")
        || !response.getBody().resultCd().equals("0000")
    ){
      throw new RestClientException("null 또는 오류 메세지를 받았습니다.");
    }
    siteCode = response.getBody().siteCode();
    tokenVersionId = response.getBody().tokenVersionId();

    return dateTime.trim() + trId.trim() + response.getBody().tokenVal().trim();
  }

  public String makeKeyByToken(String token) throws NoSuchAlgorithmException {
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(token.getBytes());
    return Base64.getEncoder().encodeToString(md.digest());
  }

  public PopUpCompositionDto makeAndReturnEncryptedDataAndIntegrityValueAndTokenVersionId (HttpSession session)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, JsonProcessingException, IllegalBlockSizeException, BadPaddingException {
    String cryptoToken = getCryptoToken(getAccessToken());
    String result = makeKeyByToken(cryptoToken);

    String key = result.substring(0, 16);
    String iv = result.substring(result.length() - 16);
    String hmacKey = result.substring(0, 32);

    session.setAttribute("key", key);
    session.setAttribute("iv", iv);
    session.setAttribute("hmacKey", hmacKey);

    SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8)));

    PlainRequestData dto = null;
    if(siteCode != null) {
      dto = PlainRequestData.builder()
          .requestno(UUID.randomUUID().toString())
          .sitecode(returnUrl)
          .sitecode(siteCode)
          .authtype("M")
          .methodtype("GET")
          .build();
    }
    String dtoStringValue = objectMapper.writeValueAsString(dto);

    byte[] encryptedByte = cipher.doFinal(dtoStringValue.trim().getBytes(StandardCharsets.UTF_8));
    String encryptedData = Base64.getEncoder().encodeToString(encryptedByte);

    Mac mac = Mac.getInstance("HmacSHA256");
    secretKey = new SecretKeySpec(hmacKey.getBytes(), "HmacSHA256");
    mac.init(secretKey);
    byte[] integrityByte = mac.doFinal(encryptedByte);
    String integrityValue = Base64.getEncoder().encodeToString(integrityByte);

    return PopUpCompositionDto.builder()
        .m("m")
        .tokenVersionId(tokenVersionId)
        .encryptedData(encryptedData)
        .integrityValue(integrityValue)
        .build();
  }

  public String integrityCheckAndDecryptData(String returnEncryptedData, String returnIntegrityValue, HttpSession session)
      throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {

    String key = (String) session.getAttribute("key");
    String iv = (String) session.getAttribute("iv");
    String hmacKey = (String) session.getAttribute("hmacKey");

    Mac mac = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKey = new SecretKeySpec(hmacKey.getBytes(), "HmacSHA256");
    mac.init(secretKey);
    byte[] integrityByte = mac.doFinal(returnEncryptedData.getBytes());
    String integrityValue = Base64.getEncoder().encodeToString(integrityByte);

    if(!integrityValue.equals(returnIntegrityValue)){
      log.info("메세지가 위조되었거나 무결성 검증 과정에서 오류가 발생하였습니다.");
      throw new IllegalArgumentException("잘못된 암호문이 도달했습니다.");
    }

    secretKey = new SecretKeySpec(key.getBytes(), "AES");
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv.getBytes()));
    String returnDecryptedData = new String(cipher.doFinal(Base64.getDecoder().decode(returnEncryptedData)), "EUC-KR");
    return returnDecryptedData;
  }
}
