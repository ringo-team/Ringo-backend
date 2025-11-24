package com.lingo.lingoproject.snap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SavePhotographerInfoRequestDto(

    @NotBlank @Schema(description = "작가 소개", example = "안녕하세요")
    String content,

    @NotBlank @Schema(description = "인스타그램 id", example = "@ringo")
    String instagramId,

    @NotBlank @Schema(description = "채팅 인트로", example = "안녕하세요")
    String chatIntro
) {

}
