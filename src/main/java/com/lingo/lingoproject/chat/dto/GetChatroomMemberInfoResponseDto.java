package com.lingo.lingoproject.chat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
public record GetChatroomMemberInfoResponseDto (
    @Schema(description = "유저 id", example = "35")
    Long userId,
    @Schema(description = "프로필 url")
    String profileUrl,
    @Schema(description = "닉네임", example = "불타는 망고")
    String nickname,
    @Schema(description = "해시태그", example = "[\"운동\", \"건강\"]")
    List<String> hashtag
){

}
