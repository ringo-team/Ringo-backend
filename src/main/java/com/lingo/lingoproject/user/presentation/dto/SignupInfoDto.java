package com.lingo.lingoproject.user.presentation.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

@Schema(description = "회원가입 시 전달되는 자격 정보")
@JsonIgnoreProperties
public record SignupInfoDto(
    @Schema(description = "사용자 이메일", example = "ringo1234")
    @NotBlank
    @Length(min = 6, max = 12)
    String loginId,

    @Schema(description = "사용자 비밀번호", example = "password1234!")
    @NotBlank
    @Length(min = 8)
    String password,

    boolean isMarketingReceptionConsent
) {
}