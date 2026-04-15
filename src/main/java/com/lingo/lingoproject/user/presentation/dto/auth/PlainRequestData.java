package com.lingo.lingoproject.user.presentation.dto.auth;

import lombok.Builder;

@Builder
public record PlainRequestData(String requestno,
                               String returnurl,
                               String sitecode,
                               String authtype,
                               String methodtype) {

  public static PlainRequestData of(String requestno, String returnurl, String sitecode, String authtype, String methodtype) {
    return PlainRequestData.builder()
        .requestno(requestno)
        .returnurl(returnurl)
        .sitecode(sitecode)
        .authtype(authtype)
        .methodtype(methodtype)
        .build();
  }
}
