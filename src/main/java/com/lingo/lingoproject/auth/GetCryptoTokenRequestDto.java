package com.lingo.lingoproject.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetCryptoTokenRequestDto {

  private dataHeader header;
  private dataBody body;

  @Builder
  public record dataHeader(
      @JsonProperty("CNTY_CD") String lang
  ){}

  @Builder
  public record dataBody(
      @JsonProperty("req_dtim") String requestDateTime,
      @JsonProperty("req_no") String requestNum,
      @JsonProperty("enc_mode") String encryptMode
  ){}

}
