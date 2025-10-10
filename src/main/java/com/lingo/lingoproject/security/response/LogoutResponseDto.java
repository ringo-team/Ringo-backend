package com.lingo.lingoproject.security.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

@Schema(description = "로그아웃 응답 정보")
public record LogoutResponseDto(
    @Schema(description = "응답 상태 코드") HttpStatus status,
    @Schema(description = "응답 메시지", example = "로그아웃 되었습니다.") String response,
    @Schema(description = "폐기된 토큰 값", example = "Bearer ...", nullable = true) String token) {

}
