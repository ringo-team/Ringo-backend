package com.lingo.lingoproject.user.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.user.presentation.dto.auth.CryptoTokenInfo;
import com.lingo.lingoproject.user.presentation.dto.auth.DecryptKeyObject;
import com.lingo.lingoproject.user.presentation.dto.auth.GetAccessTokenResponseDto;
import com.lingo.lingoproject.user.presentation.dto.auth.GetCryptoTokenRequestDto;
import com.lingo.lingoproject.user.presentation.dto.auth.GetCryptoTokenResponseDto;
import com.lingo.lingoproject.user.presentation.dto.auth.PlainRequestData;
import com.lingo.lingoproject.user.presentation.dto.auth.AuthWindowRequestDto;
import com.lingo.lingoproject.user.presentation.dto.auth.UserSelfAuthInfo;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class SelfAuthUseCase {

  private final ObjectMapper objectMapper;
  private final UserRepository userRepository;
  private final RedisUtils redisUtils;
  private final RedisTemplate<String, Object> redisTemplate;

  @Value("${self-auth.client_id}")
  private String clientId;

  @Value("${self-auth.client_secret}")
  private String clientSecret;

  @Value("${self-auth.url}")
  private String selfAuthApiUrl;

  @Value("${self-auth.return_url}")
  private String returnUrl;

  @Value("${self-auth.product_id}")
  private String productId;

  public String getAccessToken(){
    String uriPath = "/digital/niceid/oauth/oauth/token";
    String auth = "Basic " + Base64.getEncoder().encodeToString((clientId+":"+clientSecret).getBytes());

    MultiValueMap<String, Object> params = new LinkedMultiValueMap<>();
    params.add("grant_type", "client_credentials");
    params.add("scope", "default");

    WebClient webClient = WebClient.create();

    /*
     *  Authorization : Basic + Base64(clientId:clientSecret)
     *  URL?grant_type=client_credentials&scope=default
     */
    GetAccessTokenResponseDto response;
    try{
      response = webClient
          .post()
          .uri(selfAuthApiUrl + uriPath)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .header(HttpHeaders.AUTHORIZATION, auth)
          .body(BodyInserters.fromValue(params))
          .retrieve()
          .bodyToMono(GetAccessTokenResponseDto.class)
          .timeout(Duration.ofSeconds(5))
          .block();
    } catch (Exception e) {
      log.error("step=액세스_토큰_요청_실패, uri={}", uriPath, e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if (response == null) {
      log.error("step=액세스_토큰_응답_null, uri={}", uriPath);
      throw new RingoException("본인인증 api response의 값이 없습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    else if(!response.getDataHeader().resultCd().equals("1200")){
      log.error("step=액세스_토큰_응답_오류, uri={}, resultCd={}", uriPath, response.getDataHeader().resultCd());
      throw new RingoException("본인인증 api의 응답값에서 정상 토큰을 얻지 못하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    return response.getDataBody().accessToken();
  }

  /**
   * access token으로 메세지를 암호화 및 복호화하는데 필요한 토큰을 얻는다.
   */
  public CryptoTokenInfo getCryptoTokenInfo(String accessToken) throws NoSuchAlgorithmException {

    String uriPath = "digital/niceid/api/v1.0/common/crypto/token";

    long timestamp = System.currentTimeMillis()/1000;
    String auth = "bearer " + Base64.getEncoder().encodeToString((accessToken +":" + timestamp + ":" + clientId).getBytes());

    String cryptoTokenInfoCreateRequestTransactionId = UUID.randomUUID().toString();

    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    String cryptoTokenInfoCreateRequestDateTime = sdf.format(timestamp);

    /*
     *  Authorization : bearer + Base64(accessToken:timestamp:clientId)
     *  ProductId
     *  {
     *    dataHeader: {
     *         CNTY_CD : ko
     *    },
     *    dataBody: {
     *         req_dtim : dateTime (yyyyMMddHHmmss),
     *         req_no : trId,
     *         enc_mode : 1
     *    }
     *  }
     */
    GetCryptoTokenRequestDto request = GetCryptoTokenRequestDto.of(
        "ko", cryptoTokenInfoCreateRequestDateTime, cryptoTokenInfoCreateRequestTransactionId, "1");

    /*
     *  암호화 토큰 요청
     */
    GetCryptoTokenResponseDto response;
    WebClient webClient = WebClient.create();
    try{
      log.info("step=암호화_토큰_요청_시작, uri={}", uriPath);
      response = webClient
          .post()
          .uri(selfAuthApiUrl + uriPath)
          .contentType(MediaType.APPLICATION_FORM_URLENCODED)
          .header(HttpHeaders.AUTHORIZATION, auth)
          .header("ProductId", productId)
          .bodyValue(request)
          .retrieve()
          .bodyToMono(GetCryptoTokenResponseDto.class)
          .timeout(Duration.ofSeconds(5))
          .block();

    }catch (Exception e){
      log.error("step=암호화_토큰_요청_실패, uri={}", uriPath, e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    if(response == null
        || !response.getDataHeader().resultCd().equals("1200")
        || !response.getDataBody().responseCD().equals("P000")
        || response.getDataBody().responseMsg().startsWith("EAPI")
        || !response.getDataBody().resultCd().equals("0000")
    ){
      if (response != null) log.error("step=암호화_토큰_응답_오류, uri={}, resultCd={}", uriPath, response.getDataHeader().resultCd());
      throw new RingoException("본인인증 api response에서 null 또는 오류 메세지를 받았습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /*
     *  응답으로부터 토큰을 생성
     */
    String siteCode = response.getDataBody().siteCode();
    String responseCryptoTokenVersionId = response.getDataBody().tokenVersionId();
    String responseCryptoTokenValue = response.getDataBody().tokenVal();


    String token = cryptoTokenInfoCreateRequestDateTime.trim() +
        cryptoTokenInfoCreateRequestTransactionId.trim() +
        responseCryptoTokenValue.trim();
    MessageDigest hashFunction = MessageDigest.getInstance("SHA-256");
    hashFunction.update(token.getBytes());
    byte[] cryptoTokenByHashing = hashFunction.digest();
    String base64EncodedCryptoToken = Base64.getEncoder().encodeToString(cryptoTokenByHashing);

    return CryptoTokenInfo.of(siteCode, responseCryptoTokenVersionId, base64EncodedCryptoToken);
  }


  /**
   * 클라이언트에서 호출창 호출을 위한 3가지 데이터를 생성 및 전달하는 함수
   *  - token_version_id
   *  - enc_data = encrypt( sitecode, requestno, returnurl, authtype, methodtype )
   *  - integrity_value
   */
  public AuthWindowRequestDto getAuthWindowRequestData (Long userId)
      throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException, JsonProcessingException, IllegalBlockSizeException, BadPaddingException {

    CryptoTokenInfo tokenInfo = getCryptoTokenInfo(getAccessToken());
    String token = tokenInfo.getToken();

    /*
     *  요청데이터(plainRequestData)를 암호화 및 복호화하기 위한 키 생성
     *  이전에 생성했던 암호화 토큰으로 키를 생성한다.
     */
    String encryptionKey = token.substring(0, 16);
    String initialValueForEncryption = token.substring(token.length() - 16);
    String hmacKeyForIntegrityCheck  = token.substring(0, 32);

    /*
     * 나중에 복호화할 때 쓰기 위해
     * redis에 key 정보를 보관해놓는다.
     * 대칭키 암호화 방식을 사용하기 때문에 암호화 키가 복호화할 때 사용된다.
     */
    DecryptKeyObject object = DecryptKeyObject.of(encryptionKey, initialValueForEncryption, hmacKeyForIntegrityCheck);
    redisTemplate.opsForValue().set(tokenInfo.getTokenVersionId(), object, 30, TimeUnit.MINUTES);

    /*
     * 요청 데이터(plainRequestData)에는 다음과 같은 정보가 포함된다.
     *  - requestno : ringo에서 생성한 임의의 값
     *  - returnurl : 사용자 본인인증 정보를 받을 콜백 주소
     *  - sitecode : 암호화된 토큰
     *  - authtype : 인증수단인데 핸드폰(M)으로 고정
     *  - methodtype : GET 고정
     */
    PlainRequestData plainRequestData = null;
    if(tokenInfo.getSiteCode() != null) {
      plainRequestData = PlainRequestData.of(userId.toString(), returnUrl, tokenInfo.getSiteCode(), "M", "GET");
    }
    String dataStringValue = objectMapper.writeValueAsString(plainRequestData);

    /*
     * plainRequestData를 암호화하는 로직이다.
     * plainRequestData -> base64EncodedAndEncryptedRequestData
     */
    SecretKeySpec secretKeyForEncryption = new SecretKeySpec(encryptionKey.getBytes(StandardCharsets.UTF_8), "AES");
    Cipher encryptFunction = Cipher.getInstance("AES/CBC/PKCS5Padding");
    encryptFunction.init(Cipher.ENCRYPT_MODE, secretKeyForEncryption, new IvParameterSpec(initialValueForEncryption.getBytes(StandardCharsets.UTF_8)));
    byte[] encryptedRequestByte = encryptFunction.doFinal(dataStringValue.trim().getBytes(StandardCharsets.UTF_8));
    String base64EncodedAndEncryptedRequestData = Base64.getEncoder().encodeToString(encryptedRequestByte);

    /*
     * 암호화 후 무결성을 확인하기 위해 integrityValue를 추가적으로 생성하여 전달한다.
     */
    Mac hashFunction = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKeyForHashing = new SecretKeySpec(hmacKeyForIntegrityCheck.getBytes(), "HmacSHA256");
    hashFunction.init(secretKeyForHashing);
    byte[] integrityByte = hashFunction.doFinal(encryptedRequestByte);
    String integrityValue = Base64.getEncoder().encodeToString(integrityByte);

    return AuthWindowRequestDto.of("m", tokenInfo.getTokenVersionId(), base64EncodedAndEncryptedRequestData, integrityValue);
  }

  /**
   * 데이터의 무결성을 확인하고 데이터를 복호화한다.
   */
  public String validateIntegrityAndDecryptData(String tokenVersionId, String encryptedData, String originalIntegrityValue)
      throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException, InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException, UnsupportedEncodingException {

    DecryptKeyObject object = redisUtils.getDecryptKeyObject(tokenVersionId);
    String decryptionKey = object.getDecryptionKey();
    String initialValueForDecryption = object.getInitialValueForDecryption();
    String hmacKeyForIntegrityCheck = object.getHmacKeyForIntegrityCheck();

    /* encryptedData로부터 integrityValue를 생성 */
    Mac hashFunction = Mac.getInstance("HmacSHA256");
    SecretKeySpec secretKeyForHashing = new SecretKeySpec(hmacKeyForIntegrityCheck.getBytes(), "HmacSHA256");
    hashFunction.init(secretKeyForHashing);
    byte[] integrityByte = hashFunction.doFinal(encryptedData.getBytes());
    String integrityValue = Base64.getEncoder().encodeToString(integrityByte);

    /* pass 서버로부터 받은 originalIntegrityValue와 방금 생성한 integrityValue가 동일한지 확인 */
    if(!integrityValue.equals(originalIntegrityValue)){
      log.error("step=본인인증_무결성_검증_실패");
      throw new RingoException("잘못된 암호문이 도달했습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /* 데이터를 복호화하여 리턴함 */
    SecretKeySpec secretKeyForDecryption = new SecretKeySpec(decryptionKey.getBytes(), "AES");
    Cipher decryptFunction = Cipher.getInstance("AES/CBC/PKCS5Padding");
    decryptFunction.init(Cipher.DECRYPT_MODE, secretKeyForDecryption, new IvParameterSpec(initialValueForDecryption.getBytes()));
    String decryptedData = new String(decryptFunction.doFinal(Base64.getDecoder().decode(encryptedData)), "EUC-KR");
    return decryptedData;
  }

  public void deserializeAndSaveUserInfo(String data){
    UserSelfAuthInfo userSelfAuthInfo = null;
    try {
      userSelfAuthInfo = objectMapper.readValue(data, UserSelfAuthInfo.class);
    } catch (Exception e) {
      log.error("step=본인인증_정보_역직렬화_실패", e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /* 유저를 찾아서 유저 정보를 저장함*/
    User user = userRepository.findById(Long.parseLong(userSelfAuthInfo.getUserId()))
        .orElseThrow(() -> new RingoException("유저 인증 정보 저장 중 유저를 찾을 수 없습니다.",
            ErrorCode.NOT_FOUND_USER, HttpStatus.NOT_FOUND));

    if (userSelfAuthInfo.getGender().equals("1")) {
      userSelfAuthInfo.setGender("MALE");
    }else{
      userSelfAuthInfo.setGender("FEMALE");
    }
    if (userSelfAuthInfo.getNationalInfo().equals("0")){
      userSelfAuthInfo.setNationalInfo("DOMESTIC");
    }else{
      userSelfAuthInfo.setNationalInfo("FOREIGN");
    }

    user.setUserSelfAuthInfo(userSelfAuthInfo);
    userRepository.save(user);

    redisTemplate.opsForValue().set("self-auth::" + user.getId(),true, 30, TimeUnit.MINUTES);
  }
}
