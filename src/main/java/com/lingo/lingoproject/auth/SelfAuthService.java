package com.lingo.lingoproject.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Gender;
import com.lingo.lingoproject.domain.enums.Nation;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.utils.RedisUtils;
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
import java.util.Date;
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
  private final UserRepository userRepository;
  private final RedisUtils redisUtils;

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

  public CryptoTokenInfo getCryptoTokenInfo(String accessToken) throws NoSuchAlgorithmException {
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
    String siteCode = response.getBody().siteCode();
    String tokenVersionId = response.getBody().tokenVersionId();

    String token = dateTime.trim() + trId.trim() + response.getBody().tokenVal().trim();
    MessageDigest md = MessageDigest.getInstance("SHA-256");
    md.update(token.getBytes());
    String encodedToken = Base64.getEncoder().encodeToString(md.digest());

    CryptoTokenInfo info = CryptoTokenInfo.builder()
        .siteCode(siteCode)
        .tokenVersionId(tokenVersionId)
        .token(encodedToken)
        .build();

    return info;
  }


  public PopUpCompositionDto makeAndReturnEncryptedDataAndIntegrityValueAndTokenVersionId (HttpSession session)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, JsonProcessingException, IllegalBlockSizeException, BadPaddingException {
    CryptoTokenInfo info = getCryptoTokenInfo(getAccessToken());

    String token = info.getToken();

    String key = token.substring(0, 16);
    String iv = token.substring(token.length() - 16);
    String hmacKey = token.substring(0, 32);


    DecryptKeyObject object = DecryptKeyObject.builder()
        .key(key)
        .iv(iv)
        .hmacKey(hmacKey)
        .build();
    redisUtils.save(info.getTokenVersionId(), object);

    SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "AES");
    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv.getBytes(StandardCharsets.UTF_8)));

    PlainRequestData dto = null;
    if(info.getSiteCode() != null) {
      dto = PlainRequestData.builder()
          .requestno(UUID.randomUUID().toString())
          .returnurl(returnUrl)
          .sitecode(info.getSiteCode())
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
        .tokenVersionId(info.getTokenVersionId())
        .encryptedData(encryptedData)
        .integrityValue(integrityValue)
        .build();
  }

  public String integrityCheckAndDecryptData(String tokenVersionId, String returnEncryptedData, String returnIntegrityValue)
      throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {

    DecryptKeyObject object = (DecryptKeyObject) redisUtils.get(tokenVersionId);
    String key = object.getKey();
    String iv = object.getIv();
    String hmacKey = object.getHmacKey();

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

  public void deserializeAndSaveData(String data) throws Exception{
    UserInfo userInfo = null;
    Date birthday = null;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
    try {
      userInfo = objectMapper.readValue(data, UserInfo.class);
      birthday = sdf.parse(userInfo.getBirthday());
    } catch (Exception e) {
      log.info(e.getMessage());
      throw new Exception(e.getMessage());
    }
    log.info("gender format : {}", userInfo.getGender());
    Gender gender = null;
    if (userInfo.getGender().equals("1")) {
      gender = Gender.MALE;
    }else{
      gender = Gender.FEMALE;
    }
    Nation nation = null;
    if (userInfo.getNationalInfo().equals("0")){
      nation = Nation.DOMESTIC;
    }else{
      nation = Nation.FOREIGN;
    }
    User user = User.builder()
        .name(userInfo.getName())
        .phoneNumber(userInfo.getPhoneNumber())
        .mobileCarrier(userInfo.getMobileCarrier())
        .nationalInfo(nation)
        .birthday(birthday)
        .gender(gender)
        .build();
    userRepository.save(user);
  }
}
