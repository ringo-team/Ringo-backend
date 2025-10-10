package com.lingo.lingoproject.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GetCryptoTokenResponseDto {

  private dataHeader header;
  private dataBody body;

  public record dataHeader(
      @JsonProperty("GW_RSLT_CD") String resultCd,
      @JsonProperty("GW_RSLT_MSG") String resultMsg
  ){}

  public record dataBody(
      @JsonProperty("rep_cd") String responseCD,
      @JsonProperty("res_msg") String responseMsg,
      @JsonProperty("result_cd") String resultCd,
      @JsonProperty("site_code") String siteCode,
      @JsonProperty("token_version_id") String tokenVersionId,
      @JsonProperty("token_val") String tokenVal,
      int period
  ){}
}
