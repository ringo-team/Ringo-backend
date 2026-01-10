package com.lingo.lingoproject.security.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

@Schema(description = "로그인 및 회원가입 시 전달되는 자격 정보")
public record LoginInfoDto(
    @Schema(description = "사용자 이메일", example = "ringo1234")
    @NotBlank
    @Length(min = 6, max = 12)
    String loginId,

    @Schema(description = "사용자 비밀번호", example = "password1234!")
    @NotBlank
    @Length(min = 8)
    String password,

    @Schema(description = "마케팅 수신 동의 여부", example = "true")
    @NotNull
    Boolean isMarketingReceptionConsent
) {

}
