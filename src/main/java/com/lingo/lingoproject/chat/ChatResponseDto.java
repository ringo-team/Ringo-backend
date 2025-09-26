package com.lingo.lingoproject.chat;

import java.time.LocalDateTime;

public record ChatResponseDto(Long userId, String nickname, String content, LocalDateTime createdAt) {
}
