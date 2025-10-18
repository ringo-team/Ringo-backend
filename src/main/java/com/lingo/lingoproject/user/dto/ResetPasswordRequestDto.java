package com.lingo.lingoproject.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ResetPasswordRequestDto(
    @Schema(description = "패스워드", example = "ringo1234")
    String password
) {

}
