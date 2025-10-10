package com.lingo.lingoproject.user;

import com.lingo.lingoproject.domain.enums.Religion;
import lombok.Builder;

@Builder
public record GetUserInfoResponseDto(
    Long id,
    String gender,
    String email,
    String height,
    Boolean isDrinking,
    Boolean isSmoking,
    String job,
    String nickname,
    String religion
) {

}
