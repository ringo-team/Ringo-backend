package com.lingo.lingoproject.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GetCryptoTokenRequestDto {

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
