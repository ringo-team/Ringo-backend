package com.lingo.lingoproject.security.response;

import org.springframework.http.HttpStatus;

public record LogoutResponseDto(HttpStatus status,  String response, String token) {

}
