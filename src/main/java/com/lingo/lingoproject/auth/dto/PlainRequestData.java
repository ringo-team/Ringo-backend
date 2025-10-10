package com.lingo.lingoproject.auth.dto;

import lombok.Builder;

@Builder
public record PlainRequestData(String requestno,
                               String returnurl,
                               String sitecode,
                               String authtype,
                               String methodtype) {

}
