package com.lingo.lingoproject.chat.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingo.lingoproject.chat.presentation.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.matching.domain.event.MatchingAcceptedEvent;
import com.lingo.lingoproject.notification.application.FcmNotificationUseCase;
import com.lingo.lingoproject.shared.domain.model.NotificationType;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 매칭 수락 이벤트를 수신하여 채팅방을 생성한다.
 * MatchingAcceptedEvent는 matching 컨텍스트에서 발행되고
 * chat 컨텍스트에서 처리함으로써 두 컨텍스트 간의 직접 의존을 제거한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingAcceptedEventHandler {

  private final ChatService chatService;
  private final FcmNotificationUseCase fcmNotificationUseCase;
  private final UserQueryUseCase userQueryUseCase;
  private final ObjectMapper objectMapper;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void handle(MatchingAcceptedEvent event) throws Exception{
    log.info("MatchingAcceptedEvent 수신: matchingId={}, requestUser={}, requestedUser={}",
        event.getMatchingId(), event.getRequestUserId(), event.getRequestedUserId());
    CreateChatroomRequestDto dto = new CreateChatroomRequestDto(
        event.getRequestUserId(),
        event.getRequestedUserId(),
        "USER"
    );
    chatService.채팅방_생성(dto);
    User 매칭_요청_유저 = userQueryUseCase.유저_찾기_혹은_오류(event.getRequestUserId());
    User 매칭_응답_유저 = userQueryUseCase.유저_찾기_혹은_오류(event.getRequestedUserId());

    String params = objectMapper.writeValueAsString(Map.of("tab", "sent"));

    fcmNotificationUseCase.sendFcmNotification(
        매칭_요청_유저,
        매칭_응답_유저.getProfile() != null ? 매칭_응답_유저.getProfile().getImageUrl() : null,
        매칭_응답_유저.getNickname() + "(이)가 매칭이 승낙하였습니다.",
        null,
        NotificationType.MATCHING_ACCEPTED,
        "/(tabs)/like",
        params
        );
  }
}
