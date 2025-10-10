package com.lingo.lingoproject.security.controller;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "로그인 및 회원가입 시 전달되는 자격 정보")
public record LoginInfoDto(
    @Schema(description = "사용자 이메일", example = "user@ringolinkgo.com")
    String email,
    @Schema(description = "사용자 비밀번호", example = "password1234!")
    String password
) {

}
