package com.lingo.lingoproject.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GetAccessTokenResponseDto {

  private dataHeader header;
  private dataBody body;

  public record dataHeader(
      @JsonProperty("GW_RSLT_CD") String resultCd,
      @JsonProperty("GW_RSLT_MSG") String resultMsg,
      @JsonProperty("TRAN_ID") String trId
  ){}
  public record dataBody(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("expires_in") int expiresIn,
      @JsonProperty("token_type") String tokenType,
      @JsonProperty("scope") String scope
  ){}
}
