package com.lingo.lingoproject.user.presentation.dto.auth;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserSelfAuthInfo {

  @JsonProperty("requestno")
  private String userIdentity;

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
