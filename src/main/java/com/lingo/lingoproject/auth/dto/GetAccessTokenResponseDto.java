package com.lingo.lingoproject.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;

@Data
public class GetAccessTokenResponseDto {

  private DataHeader dataHeader;
  private DataBody dataBody;

  public record DataHeader(
      @JsonProperty("GW_RSLT_CD") String resultCd,
      @JsonProperty("GW_RSLT_MSG") String resultMsg,
      @JsonProperty("TRAN_ID") String trId
  ){}
  public record DataBody(
      @JsonProperty("access_token") String accessToken,
      @JsonProperty("expires_in") int expiresIn,
      @JsonProperty("token_type") String tokenType,
      @JsonProperty("scope") String scope
  ){}
}
