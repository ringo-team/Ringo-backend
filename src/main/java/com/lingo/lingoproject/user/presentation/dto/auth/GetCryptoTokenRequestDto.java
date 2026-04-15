package com.lingo.lingoproject.user.presentation.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetCryptoTokenRequestDto {

  public static GetCryptoTokenRequestDto of(String lang, String requestDateTime, String requestNum, String encryptMode) {
    return GetCryptoTokenRequestDto.builder()
        .dataHeader(DataHeader.builder().lang(lang).build())
        .dataBody(DataBody.builder()
            .requestDateTime(requestDateTime)
            .requestNum(requestNum)
            .encryptMode(encryptMode)
            .build())
        .build();
  }

  private DataHeader dataHeader;
  private DataBody dataBody;

  @Builder
  public record DataHeader(
      @JsonProperty("CNTY_CD") String lang
  ){}

  @Builder
  public record DataBody(
      @JsonProperty("req_dtim") String requestDateTime,
      @JsonProperty("req_no") String requestNum,
      @JsonProperty("enc_mode") String encryptMode
  ){}

}
