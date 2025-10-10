package com.lingo.lingoproject.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInfo {

  @JsonProperty("mobileco")
  private String mobileCarrier;

  @JsonProperty("mobileno")
  private String phoneNumber;

  @JsonProperty("utf8_name")
  private String name;

  @JsonProperty("nationalinfo")
  private String nationalInfo;

  private String birthday;

  private String gender;
}
