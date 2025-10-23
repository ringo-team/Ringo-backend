package com.lingo.lingoproject.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record GetFriendInvitationCodeRequestDto(
    @Schema(description = "친구초대코드", example = "XZ3RT5F3")
    String code) {

}
