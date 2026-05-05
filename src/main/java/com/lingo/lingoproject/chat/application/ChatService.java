package com.lingo.lingoproject.chat.application;

import com.lingo.lingoproject.chat.presentation.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatMessageResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.ChatOpponentInfoDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.SaveAppointmentRequestDto;
import com.lingo.lingoproject.shared.domain.model.Appointment;
import com.lingo.lingoproject.shared.domain.model.ChatType;
import com.lingo.lingoproject.shared.domain.model.Chatroom;
import com.lingo.lingoproject.shared.domain.model.ChatroomParticipant;
import com.lingo.lingoproject.shared.domain.model.FailedChatMessageLog;
import com.lingo.lingoproject.shared.domain.model.Matching;
import com.lingo.lingoproject.shared.domain.model.MatchingStatus;
import com.lingo.lingoproject.shared.domain.model.Message;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.AppointmentRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ChatroomParticipantRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ChatroomRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.FailedChatMessageLogRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.MatchingRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.MessageRepository;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import com.lingo.lingoproject.shared.utils.GenericUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 채팅방 및 메시지 관련 비즈니스 로직을 담당하는 서비스.
 *
 * <h2>주요 기능</h2>
 * <ul>
 *   <li>채팅방 생성/삭제 — 매칭 수락(ACCEPTED) 상태인 두 사용자 간에만 생성 가능</li>
 *   <li>채팅 메시지 조회 — 페이지네이션 + 읽음/미읽음 상태 표시</li>
 *   <li>채팅방 목록 조회 — 나간(isActive=false) 채팅방은 목록에서 제외</li>
 *   <li>WebSocket 접속 상태 확인 — Redis {@code connect::{userId}::{chatroomId}} 키로 현재 접속자 파악</li>
 *   <li>약속 생성 — 채팅방 참여자만 약속을 잡을 수 있음</li>
 *   <li>메시지 전송 실패 이력 기록 — {@link FailedChatMessageLog}로 추적</li>
 * </ul>
 *
 * <h2>WebSocket 접속 상태 감지</h2>
 * <p>사용자가 특정 채팅방에 STOMP SUBSCRIBE하면
 * {@code connect::{userId}::{chatroomId}} 키가 Redis에 저장됩니다.
 * {@link #getConnectedUserIds}는 이 키의 존재 여부로 현재 채팅방에 연결된 사용자를 판별합니다.</p>
 *
 * <h2>탈퇴 사용자 처리</h2>
 * <p>{@link ChatroomParticipant#isWithdrawn()}이 true인 참여자가 있으면
 * 상대방 정보를 "알 수 없음"으로 마스킹하여 반환합니다.</p>
 *
 * <h2>읽음 상태 규칙</h2>
 * <p>메시지의 {@code readerIds}에 상대방 ID가 포함되어 있으면 읽음(isRead=1), 없으면 미읽음(isRead=0)으로 표시합니다.
 * 발신자 자신은 항상 readerIds에 포함되어야 합니다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

  private static final DateTimeFormatter CHAT_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final ChatroomRepository chatroomRepository;
  private final MessageRepository messageRepository;
  private final UserQueryUseCase userQueryUseCase;
  private final ChatroomParticipantRepository chatroomParticipantRepository;
  private final MatchingRepository matchingRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final FailedChatMessageLogRepository failedChatMessageLogRepository;
  private final AppointmentRepository appointmentRepository;

  // ============================================================
  // Public API — 채팅 메시지
  // ============================================================

  @Transactional
  public GetChatResponseDto getChatMessages(
      User user,
      Long chatroomId,
      int page,
      int size
  ) {

    if (page < 0 || size < 1 || size > 100) throw new RingoException("잘못된 페이지 요청입니다.", ErrorCode.BAD_REQUEST);
    if (chatroomId == null) throw new RingoException("채팅방 ID가 없습니다.", ErrorCode.BAD_REQUEST);

    validateParticipant(chatroomId, user.getId());

    Chatroom chatroom = findChatroomOrThrow(chatroomId);

    List<GetChatMessageResponseDto> messages = loadPagedMessages(chatroomId, page, size);
    messageRepository.readAllMessages(chatroomId, user.getId());

    ChatroomParticipant chatOpponent = chatroom.getOpponent(user);

    if (chatOpponent.isWithdrawn()) return GetChatResponseDto.of(ChatOpponentInfoDto.withdrawn(), messages);

    // 로그

    return GetChatResponseDto.of(ChatOpponentInfoDto.from(chatOpponent.getParticipant()), messages);
  }

  public Message saveMessage(GetChatMessageResponseDto messageDto, Long chatroomId) {
    return messageRepository.save(Message.of(chatroomId, messageDto));
  }

  public List<Long> getConnectedUserIds(List<User> roomMembers, Long chatroomId) {
    try {
      List<Long> connectedIds = roomMembers.stream()
          .map(User::getId)
          .filter(memberId -> redisTemplate.hasKey("connect::" + memberId + "::" + chatroomId))
          .toList();

      log.info("chatroomId={}, connectedUserIds={}", chatroomId, connectedIds);
      return connectedIds;

    } catch (Exception e) {
      log.error("step=접속자_조회_실패, chatroomId={}, status=FAILED", chatroomId, e);
      throw new RingoException("채팅방 접속자 조회 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  public void recordMessageDeliveryFailure(
      Exception e,
      Message savedMessage,
      String senderLoginId,
      String destination
  ) {
    log.error("step=메시지_전송_실패, destination={}, senderLoginId={}, status=FAILED",
        destination, senderLoginId, e);

    Long chatroomId = extractChatroomIdFromDestination(destination);
    failedChatMessageLogRepository.save(
        FailedChatMessageLog.of(
            chatroomId, e, savedMessage.getId(), destination, senderLoginId));
  }

  // ============================================================
  // Public API — 채팅방 관리
  // ============================================================

  public List<GetChatroomResponseDto> getChatroomsByUser(User user) {
    List<Chatroom> chatrooms = chatroomRepository.findAllByUser(user);
    List<Long> chatroomIds = getChatroomIds(chatrooms);

    Map<Long, ChatroomSummaryProjection> summaryMap = messageRepository
        .findChatroomSummaries(chatroomIds, user.getId())
        .stream()
        .collect(Collectors.toMap(ChatroomSummaryProjection::getId, p -> p));

    return chatrooms.stream()
        .filter(chatroom -> chatroom.userIsActive(user))
        .map(chatroom -> buildChatroomResponseDto(chatroom, user, summaryMap.get(chatroom.getId())))
        .toList();
  }

  private GetChatroomResponseDto buildChatroomResponseDto(
      Chatroom chatroom,
      User user,
      ChatroomSummaryProjection summary
  ){
    ChatroomParticipant participant = chatroom.getOpponent(user);
    User opponent;

    if (participant.isWithdrawn()) opponent = null;
    else opponent = participant.getParticipant();

    return GetChatroomResponseDto.of(chatroom, opponent, summary);
  }

  private List<Long> getChatroomIds(List<Chatroom> chatrooms){
    return chatrooms.stream().map(Chatroom::getId).toList();
  }

  @Transactional
  public Chatroom createChatroom(CreateChatroomRequestDto dto) {
    ChatType chatType = GenericUtils.validateAndReturnEnumValue(ChatType.values(), dto.chatType());

    User user1 = findUserOrThrow(dto.user1Id());
    User user2 = findUserOrThrow(dto.user2Id());

    validateUsersAreMatched(user1, user2);

    long minId = Math.min(user1.getId(), user2.getId());
    long maxId = Math.max(user1.getId(), user2.getId());
    String chatroomName = minId + "_" + maxId;

    Chatroom savedChatroom = chatroomRepository.save(Chatroom.of(chatroomName, chatType));
    chatroomParticipantRepository.saveAll(List.of(
        ChatroomParticipant.of(user1, savedChatroom),
        ChatroomParticipant.of(user2, savedChatroom)
    ));

    log.info("""
            step=채팅방_생성,
            user1Id={}, user1Nickname={}, user1Gender={},
            user1Birthday={}, user2Id={}, user2Nickname={},
            user2Gender={}, user2Birthday={}, chatType={}, createdAt={}
            """,
        user1.getId(), user1.getNickname(), user1.getGender(), user1.getBirthday(),
        user2.getId(), user2.getNickname(), user2.getGender(), user2.getBirthday(),
        savedChatroom.getType(), savedChatroom.getCreatedDate());

    return savedChatroom;
  }

  @Transactional
  public void deleteChatroom(Long chatroomId, User user) {

    validateParticipant(chatroomId, user.getId());

    Chatroom chatroom = findChatroomOrThrow(chatroomId);
    chatroomParticipantRepository.deleteAllByChatroom(chatroom);
    messageRepository.deleteAllByChatroomId(chatroomId);
    chatroomRepository.delete(chatroom);
  }

  public void validateParticipant(Long chatroomId, Long userId) {
    Chatroom chatroom = findChatroomOrThrow(chatroomId);
    User user = findUserOrThrow(userId);
    if (!chatroom.isParticipant(user)) {
      log.error("chatroomId={}, userId={}, step=채팅방_접근_권한_없음", chatroomId, userId);
      throw new RingoException("채팅방에 소속되지 않은 유저입니다.", ErrorCode.NO_AUTH);
    }
  }

  public List<User> getNonWithdrawnParticipantUsers(Long chatroomId) {
    Chatroom chatroom = findChatroomOrThrow(chatroomId);
    return chatroom.getNonWithdrawnParticipant();
  }

  // ============================================================
  // Public API — 약속
  // ============================================================

  public GetChatMessageResponseDto createAppointment(SaveAppointmentRequestDto dto) {
    User register = findUserOrThrow(dto.registerId());
    Chatroom chatroom = findChatroomOrThrow(dto.chatroomId());

    validateParticipant(dto.chatroomId(), dto.registerId());

    log.info("""
        step=약속_생성,
        chatroomId={}, registrantNickname={}, place={},
        appointmentTime={}, isAlert={}, alertTime={}""",
        chatroom.getId(), register.getNickname(),
        dto.place(), dto.appointmentTime(),
        dto.isAlert() == 1, dto.alertTime());

    Appointment savedAppointment = appointmentRepository.save(Appointment.of(chatroom, register, dto));

    return GetChatMessageResponseDto.forAppointment(savedAppointment);
  }

  public List<Appointment> getDueAppointments() {
    return appointmentRepository.findAllByAlertTimeBeforeAndIsAlert(LocalDateTime.now(), true);
  }

  public void saveAppointments(List<Appointment> appointments) {
    appointmentRepository.saveAll(appointments);
  }

  // ============================================================
  // Public API — 사용자 / 유틸
  // ============================================================

  public User findUserOrThrow(Long userId) {
    return userQueryUseCase.findUserOrThrow(userId);
  }

  public User findUserByLoginIdOrThrow(String loginId) {
    return userQueryUseCase.findByLoginId(loginId)
        .orElseThrow(() -> new RingoException("유저 정보가 올바르지 않습니다.", ErrorCode.FORBIDDEN));
  }

  /** destination 경로의 마지막 세그먼트에서 채팅방 ID를 추출한다. 예: "/user/queue/topic/42" → 42L */
  public Long extractChatroomIdFromDestination(String destination) {
    if (destination == null) return null;
    String[] parts = destination.split("/");
    if (parts.length == 0) return null;
    try {
      return Long.valueOf(parts[parts.length - 1]);
    } catch (Exception e) {
      return null;
    }
  }

  // ============================================================
  // Private helpers
  // ============================================================

  private Chatroom findChatroomOrThrow(Long chatroomId) {
    return chatroomRepository.findById(chatroomId)
        .orElseThrow(() -> new RingoException(
            "채팅방 찾을 수 없음",
            ErrorCode.BAD_PARAMETER));
  }

  private void validateUsersAreMatched(User user1, User user2) {
    boolean matched =
        matchingRepository.existsByRequestUserAndRequestedUserAndMatchingStatus(
            user1, user2, MatchingStatus.ACCEPTED)
        || matchingRepository.existsByRequestUserAndRequestedUserAndMatchingStatus(
            user2, user1, MatchingStatus.ACCEPTED);

    if (!matched) {
      logMatchingCreationError(user1, user2);
      throw new RingoException(
          "매칭되지 않은 쌍은 채팅방을 생성할 수 없습니다.",
          ErrorCode.NO_AUTH);
    }
  }

  private void logMatchingCreationError(User user1, User user2) {
    Matching matching1 = matchingRepository.findFirstByRequestUserAndRequestedUser(user1, user2);
    Matching matching2 = matchingRepository.findFirstByRequestUserAndRequestedUser(user2, user1);

    if (matching1 == null && matching2 == null) {
      log.error("user1Id={}, user2Id={}, step=매칭_이력_없음", user1.getId(), user2.getId());
      return;
    }
    if (matching1 == null) {
      logChatroomCreationFailure(user2, user1, matching2.getMatchingStatus());
      return;
    }
    if (matching2 == null) {
      logChatroomCreationFailure(user1, user2, matching1.getMatchingStatus());
      return;
    }
    log.error("user1Id={}, user2Id={}, step=양방향_매칭_미승인", user1.getId(), user2.getId());
    logChatroomCreationFailure(user1, user2, matching1.getMatchingStatus());
    logChatroomCreationFailure(user2, user1, matching2.getMatchingStatus());
  }

  private void logChatroomCreationFailure(User requestUser, User requestedUser, MatchingStatus status) {
    log.error("step=채팅방_생성_실패, requestUserId={}, requestedUserId={}, matchingStatus={}",
        requestUser.getId(), requestedUser.getId(), status);
  }

  // edge case
  private List<GetChatMessageResponseDto> loadPagedMessages(Long chatroomId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return messageRepository.findAllByChatroomIdOrderByCreatedAtDesc(chatroomId, pageable)
        .stream()
        .map(GetChatMessageResponseDto::from)
        .toList();
  }
}
