package com.lingo.lingoproject.chat.dto;

import java.util.List;
import lombok.Builder;

@Builder
public record GetChatroomMemberInfoResponseDto (
    Long userId,
    String profileUrl,
    String nickname,
    List<String> hashtag
){

}
