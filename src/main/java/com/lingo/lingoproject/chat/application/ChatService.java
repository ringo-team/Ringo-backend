package com.lingo.lingoproject.chat.application;

import com.lingo.lingoproject.chat.presentation.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatMessageResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatroomMemberInfoResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.chat.presentation.dto.SaveAppointmentRequestDto;
import com.lingo.lingoproject.shared.domain.model.Appointment;
import com.lingo.lingoproject.shared.domain.model.ChatType;
import com.lingo.lingoproject.shared.domain.model.Chatroom;
import com.lingo.lingoproject.shared.domain.model.ChatroomParticipant;
import com.lingo.lingoproject.shared.domain.model.FailedChatMessageLog;
import com.lingo.lingoproject.shared.domain.model.Hashtag;
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
import com.lingo.lingoproject.shared.infrastructure.persistence.HashtagRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.MatchingRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.MessageRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.utils.GenericUtils;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
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
 * {@link #findConnectedUserIds}는 이 키의 존재 여부로 현재 채팅방에 연결된 사용자를 판별합니다.</p>
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

  /** 채팅방 목록에서 마지막 메시지 시간을 표시할 때 사용하는 날짜 포맷. */
  private static final DateTimeFormatter CHAT_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

  private final ChatroomRepository chatroomRepository;
  private final MessageRepository messageRepository;
  private final UserRepository userRepository;
  private final ChatroomParticipantRepository chatroomParticipantRepository;
  private final MatchingRepository matchingRepository;
  private final RedisTemplate<String, Object> redisTemplate;
  private final HashtagRepository hashtagRepository;
  private final FailedChatMessageLogRepository failedChatMessageLogRepository;
  private final AppointmentRepository appointmentRepository;

  // ============================================================
  // Public API
  // ============================================================

  /**
   * 채팅방 메시지를 페이지 단위로 조회한다.
   *
   * <p>요청자가 채팅방 참여자인지 검증한 뒤, 상대방 정보와 함께 메시지 목록을 반환합니다.
   * 상대방이 탈퇴한 경우 닉네임·프로필을 "알 수 없음"으로 마스킹합니다.</p>
   *
   * @param user      요청 사용자 (JWT 인증된 현재 사용자)
   * @param chatroomId 조회할 채팅방 ID
   * @param page      페이지 번호 (0부터 시작)
   * @param size      페이지당 메시지 수
   * @return 상대방 정보 + 메시지 목록 + 각 메시지의 읽음 상태
   */
  public GetChatResponseDto fetchChatMessages(User user, Long chatroomId, int page, int size) {
    validateParticipant(chatroomId, user.getId());

    Chatroom chatroom = findChatroomOrThrow(chatroomId);
    List<GetChatMessageResponseDto> messages = fetchPagedMessages(chatroomId, page, size);
    List<ChatroomParticipant> participants = chatroomParticipantRepository.findAllByChatroom(chatroom);

    if (hasWithdrawnParticipant(participants)) {
      return buildWithdrawnUserChatroomResponse(messages, user);
    }

    User opponent = resolveOpponent(participants, user);
    messages.forEach(dto -> applyReadStatus(dto, user));

    List<String> hashtags = hashtagRepository.findAllByUser(opponent).stream()
        .map(Hashtag::getHashtag).toList();

    log.info("step=메세지_조회, chatroomId={}, requestUserId={}, opponentId={}, opponentNickname={}, hashtags={}",
        chatroomId, user.getId(), opponent.getId(), opponent.getNickname(), hashtags);

    return GetChatResponseDto.of(buildOpponentMemberInfo(opponent, hashtags), messages);
  }

  /**
   * 사용자가 속한 모든 채팅방 목록을 조회한다.
   *
   * <p>나간 채팅방(isActive=false)은 목록에서 제외됩니다.
   * 탈퇴한 상대방이 있는 채팅방은 "알 수 없음"으로 마스킹된 상태로 포함됩니다.</p>
   *
   * @param user 채팅방 목록을 조회할 사용자
   * @return 채팅방 요약 목록 (상대방 닉네임, 프로필, 미읽음 수, 마지막 메시지)
   */
  public List<GetChatroomResponseDto> findChatroomsByUser(User user) {
    List<Chatroom> chatrooms = chatroomRepository.findAllByUser(user);
    List<GetChatroomResponseDto> result = new ArrayList<>();

    for (Chatroom chatroom : chatrooms) {
      GetChatroomResponseDto summary = buildChatroomSummary(chatroom, user);
      if (summary != null) {
        result.add(summary);
      }
    }

    log.info("userId={}, chatroomCount={}", user.getId(), result.size());
    return result;
  }

  /**
   * 두 사용자 간의 채팅방을 생성한다.
   *
   * <p>매칭 수락(ACCEPTED) 상태인 두 사용자만 채팅방을 생성할 수 있습니다.
   * 일반적으로 {@link MatchingAcceptedEventHandler}가 매칭 수락 이벤트를 받아 자동으로 호출합니다.</p>
   *
   * @param dto user1Id, user2Id, chatType을 포함한 채팅방 생성 요청 DTO
   * @return 생성된 채팅방 엔티티
   * @throws RingoException 두 사용자가 ACCEPTED 매칭 관계가 아닌 경우
   */
  public Chatroom createChatroom(CreateChatroomRequestDto dto) {
    ChatType chatType = GenericUtils.validateAndReturnEnumValue(ChatType.values(), dto.chatType());

    User user1 = findUserOrThrow(dto.user1Id());
    User user2 = findUserOrThrow(dto.user2Id());

    validateUsersAreMatched(user1, user2);

    Chatroom savedChatroom = saveNewChatroom(user1, user2, chatType);
    saveParticipants(savedChatroom, user1, user2);

    log.info("step=채팅방_생성, user1Id={}, user1Nickname={}, user1Gender={}, user1Birthday={}, user2Id={}, user2Nickname={}, user2Gender={}, user2Birthday={}, chatType={}, createdAt={}",
        user1.getId(), user1.getNickname(), user1.getGender(), user1.getBirthday(),
        user2.getId(), user2.getNickname(), user2.getGender(), user2.getBirthday(),
        savedChatroom.getType(), savedChatroom.getCreatedDate());

    return savedChatroom;
  }

  /**
   * 채팅방을 삭제한다. (참여자 → 메시지 → 채팅방 순으로 제거)
   *
   * <p>채팅방 참여자만 삭제할 수 있으며, 관련 데이터(ChatroomParticipant, Message)를
   * 먼저 삭제한 후 채팅방을 제거합니다.</p>
   *
   * @param chatroomId 삭제할 채팅방 ID
   * @param user       요청 사용자 (참여자 검증에 사용)
   * @throws RingoException 채팅방 참여자가 아닌 경우
   */
  @Transactional
  public void deleteChatroom(Long chatroomId, User user) {
    Chatroom chatroom = findChatroomOrThrow(chatroomId);
    validateParticipant(chatroomId, user.getId());

    chatroomParticipantRepository.deleteAllByChatroom(chatroom);
    messageRepository.deleteAllByChatroomId(chatroomId);
    chatroomRepository.delete(chatroom);
  }

  /**
   * 채팅 메시지를 MongoDB에 저장한다.
   *
   * <p>STOMP 메시지 핸들러({@code ChatController})에서 메시지를 수신한 직후 호출됩니다.
   * Message는 MongoDB에 저장되고, 이후 STOMP를 통해 상대방에게 전달됩니다.</p>
   *
   * @param messageDto STOMP로 수신된 메시지 DTO (senderId, content, readerIds 포함)
   * @param chatroomId 메시지가 전송된 채팅방 ID
   * @return 저장된 Message 엔티티
   */
  public Message persistChatMessage(GetChatMessageResponseDto messageDto, Long chatroomId) {
    return messageRepository.save(
        Message.of(chatroomId, messageDto.getSenderId(), messageDto.getContent(), messageDto.getReaderIds()));
  }

  /**
   * STOMP 메시지 전송 실패 이력을 기록한다.
   *
   * <p>메시지 저장 또는 STOMP 전달 도중 예외가 발생했을 때 호출됩니다.
   * 실패 원인, 메시지 ID, 대상 destination 등을 {@link FailedChatMessageLog}에 기록하여
   * 후속 분석 및 재처리에 활용합니다.</p>
   *
   * @param e           발생한 예외
   * @param savedMessage 저장된 메시지 엔티티 (null 가능 — 저장 전 실패한 경우)
   * @param chatroomId  채팅방 ID
   * @param senderLoginId 발신자 로그인 ID
   * @param destination STOMP destination 접두사 (예: {@code /user/queue/chatroom/})
   */
  public void recordMessageDeliveryFailure(
      Exception e,
      Message savedMessage,
      Long chatroomId,
      String senderLoginId,
      String destination
  ) {
    log.error("step=메시지_전송_실패, chatroomId={}, senderLoginId={}, status=FAILED",
        chatroomId, senderLoginId, e);

    failedChatMessageLogRepository.save(
        FailedChatMessageLog.of(chatroomId, e, savedMessage != null ? savedMessage.getId() : null,
            destination + chatroomId, senderLoginId));
  }

  /**
   * 채팅방에 현재 WebSocket으로 접속 중인 사용자 ID 목록을 조회한다.
   */
  public List<Long> findConnectedUserIds(List<User> roomMembers, Long chatroomId) {
    try {
      List<Long> connectedIds = roomMembers.stream()
          .filter(member -> Boolean.TRUE.equals(
              redisTemplate.hasKey("connect::" + member.getId() + "::" + chatroomId)
          ))
          .map(User::getId)
          .toList();

      log.info("chatroomId={}, connectedUserIds={}", chatroomId, connectedIds);
      return connectedIds;

    } catch (Exception e) {
      log.error("step=접속자_조회_실패, chatroomId={}, status=FAILED", chatroomId, e);
      throw new RingoException(
          "채팅방 접속자 조회 중 오류가 발생했습니다.",
          ErrorCode.INTERNAL_SERVER_ERROR,
          HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * 채팅방의 모든 미읽음 메시지를 읽음으로 처리한다.
   */
  @Transactional
  public void readAllMessages(Long chatroomId, Long userId) {
    messageRepository.readAllMessages(chatroomId, userId);
  }

  /**
   * 약속을 저장하고 채팅 메시지 형태의 DTO를 반환한다.
   */
  public GetChatMessageResponseDto createAppointment(SaveAppointmentRequestDto dto) {
    User registrant = findUserOrThrow(dto.registerId());
    Chatroom chatroom = findChatroomOrThrow(dto.chatroomId());

    if (!isParticipant(chatroom.getId(), registrant.getId())) {
      throw new RingoException("채팅방 멤버만 약속을 잡을 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    log.info("step=약속_생성, chatroomId={}, registrantNickname={}, place={}, appointmentTime={}, isAlert={}, alertTime={}",
        chatroom.getId(), registrant.getNickname(),
        dto.place(), dto.appointmentTime(),
        dto.isAlert() == 1, dto.alertTime());

    Appointment savedAppointment = appointmentRepository.save(
        Appointment.of(chatroom, registrant, dto.place(),
            LocalDateTime.parse(dto.appointmentTime()),
            dto.isAlert() == 1 ? LocalDateTime.parse(dto.alertTime()) : null,
            dto.isAlert() == 1));

    return GetChatMessageResponseDto.forAppointment(savedAppointment);
  }

  /**
   * 알림 시간이 도래한 약속 목록을 조회한다.
   */
  public List<Appointment> findDueAppointments() {
    return appointmentRepository.findAllByAlertTimeBeforeAndIsAlert(LocalDateTime.now(), true);
  }

  /**
   * 약속 목록을 일괄 저장한다.
   */
  public void persistAppointments(List<Appointment> appointments) {
    appointmentRepository.saveAll(appointments);
  }

  /**
   * 사용자가 해당 채팅방의 참여자인지 확인한다.
   */
  public boolean isParticipant(Long chatroomId, Long userId) {
    List<Long> participantIds = findActiveParticipants(chatroomId).stream()
        .map(User::getId).toList();
    logParticipantCheckResult(chatroomId, userId, participantIds);
    return participantIds.contains(userId);
  }

  /**
   * 채팅방의 탈퇴하지 않은 참여자 목록을 조회한다.
   */
  public List<User> findActiveParticipants(Long chatroomId) {
    Chatroom chatroom = chatroomRepository.findById(chatroomId)
        .orElseThrow(() -> new RingoException(
            "채팅방이 존재하지 않습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
    return chatroomParticipantRepository.findAllByChatroom(chatroom).stream()
        .filter(p -> !p.isWithdrawn())
        .map(ChatroomParticipant::getParticipant)
        .toList();
  }

  // ============================================================
  // Private helpers — chatroom summary
  // ============================================================

  /** 단일 채팅방 요약 DTO를 생성한다. 나간 채팅방이면 null을 반환한다. */
  private GetChatroomResponseDto buildChatroomSummary(Chatroom chatroom, User user) {
    List<ChatroomParticipant> participants = chatroomParticipantRepository.findAllByChatroom(chatroom);

    if (participants == null || participants.isEmpty()) {
      log.error("chatroomId={}, step=참여자_없는_채팅방", chatroom.getId());
      return null;
    }

    validateParticipant(chatroom.getId(), user.getId());

    String opponentNickname;
    String opponentProfileUrl = null;

    if (hasWithdrawnParticipant(participants)) {
      log.info("chatroomId={}, step=탈퇴_유저_채팅방", chatroom.getId());
      opponentNickname = "알 수 없음";
    } else {
      OpponentProfile opponentProfile = resolveOpponentProfile(participants, user);
      if (opponentProfile == null) return null;
      opponentNickname = opponentProfile.nickname();
      opponentProfileUrl = opponentProfile.imageUrl();
    }

    return buildChatroomResponseDto(chatroom, opponentNickname, opponentProfileUrl, user);
  }

  /** 참여자 목록에서 상대방의 닉네임/프로필 정보를 추출한다. 사용자가 나간 채팅방이면 null을 반환한다. */
  private OpponentProfile resolveOpponentProfile(List<ChatroomParticipant> participants, User user) {
    ChatroomParticipant participant1 = participants.get(0);
    ChatroomParticipant participant2 = participants.get(1);
    User firstUser = participant1.getParticipant();
    User secondUser = participant2.getParticipant();

    boolean isCurrentUserFirst = user.getId().equals(firstUser.getId());
    boolean isActive = isCurrentUserFirst ? participant1.isActive() : participant2.isActive();

    if (!isActive) {
      log.info("userId={}, step=나간_채팅방_제외", user.getId());
      return null;
    }

    User opponent = isCurrentUserFirst ? secondUser : firstUser;

    log.info("step=채팅방_상대방_조회, user1Id={}, user1Nickname={}, user2Id={}, user2Nickname={}, requestUserId={}, opponentId={}",
        firstUser.getId(), firstUser.getNickname(),
        secondUser.getId(), secondUser.getNickname(),
        user.getId(), opponent.getId());

    return new OpponentProfile(opponent.getNickname(), opponent.getProfile().getImageUrl());
  }

  /** 채팅방 목록 응답 DTO를 빌드한다. */
  private GetChatroomResponseDto buildChatroomResponseDto(
      Chatroom chatroom,
      String opponentNickname,
      String opponentProfileUrl,
      User user
  ) {
    int unreadCount = messageRepository.findNumberOfNotReadMessages(chatroom.getId(), user.getId());
    Optional<Message> lastMessage =
        messageRepository.findFirstByChatroomIdOrderByCreatedAtDesc(chatroom.getId());
    String lastSentAt = lastMessage.map(m -> CHAT_TIME_FORMATTER.format(m.getCreatedAt())).orElse(null);

    GetChatroomResponseDto response = GetChatroomResponseDto.of(chatroom, opponentNickname,
        opponentProfileUrl, lastMessage.map(Message::getContent).orElse(null), unreadCount, lastSentAt);

    log.info("step=채팅방_요약_빌드, chatroomId={}, opponentNickname={}, unreadCount={}, lastSentAt={}",
        response.chatroomId(), response.chatOpponent(),
        response.numberOfNotReadMessages(), response.lastSendDateTime());

    return response;
  }

  // ============================================================
  // Private helpers — chatroom creation
  // ============================================================

  /** 두 사용자가 매칭 수락 상태인지 검증한다. */
  private void validateUsersAreMatched(User user1, User user2) {
    boolean matched =
        matchingRepository.existsByRequestUserAndRequestedUserAndMatchingStatus(
            user1, user2, MatchingStatus.ACCEPTED)
        || matchingRepository.existsByRequestUserAndRequestedUserAndMatchingStatus(
            user2, user1, MatchingStatus.ACCEPTED);

    if (!matched) {
      logMatchingStatusError(user1, user2);
      throw new RingoException(
          "매칭되지 않은 쌍은 채팅방을 생성할 수 없습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }
  }

  private Chatroom saveNewChatroom(User user1, User user2, ChatType chatType) {
    return chatroomRepository.save(Chatroom.of(user1.getId() + "_" + user2.getId(), chatType));
  }

  private void saveParticipants(Chatroom chatroom, User user1, User user2) {
    chatroomParticipantRepository.saveAll(List.of(
        ChatroomParticipant.of(user1, chatroom),
        ChatroomParticipant.of(user2, chatroom)
    ));
  }

  /** 채팅방 생성 실패 원인을 매칭 상태 기준으로 로깅한다. */
  private void logMatchingStatusError(User user1, User user2) {
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

  // ============================================================
  // Private helpers — message / read status
  // ============================================================

  private List<GetChatMessageResponseDto> fetchPagedMessages(Long chatroomId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    Page<Message> messages =
        messageRepository.findAllByChatroomIdOrderByCreatedAtDesc(chatroomId, pageable);
    return messages.stream()
        .map(m -> GetChatMessageResponseDto.from(chatroomId, m))
        .toList();
  }

  /** 메시지 DTO에 읽음/미읽음 상태를 적용한다. */
  private void applyReadStatus(GetChatMessageResponseDto dto, User viewer) {
    List<Long> readerIds = dto.getReaderIds();

    if (!readerIds.contains(dto.getSenderId())) {
      log.error("readerIds={}, senderId={}, step=보낸사람_미포함", readerIds, dto.getSenderId());
      throw new RingoException(
          "readerIds에 보낸 사람의 id가 존재하지 않습니다.", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
    }
    boolean onlySenderRead = readerIds.size() == 1 && dto.getSenderId().equals(viewer.getId());
    dto.setIsRead(onlySenderRead ? 0 : 1);
  }

  // ============================================================
  // Private helpers — misc
  // ============================================================

  private void validateParticipant(Long chatroomId, Long userId) {
    if (!isParticipant(chatroomId, userId)) {
      log.error("chatroomId={}, userId={}, step=채팅방_접근_권한_없음", chatroomId, userId);
      throw new RingoException("채팅방에 소속되지 않은 유저입니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
  }

  private Chatroom findChatroomOrThrow(Long chatroomId) {
    return chatroomRepository.findById(chatroomId)
        .orElseThrow(() -> new RingoException(
            "채팅방을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
  }

  private User findUserOrThrow(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new RingoException(
            "유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));
  }

  private boolean hasWithdrawnParticipant(List<ChatroomParticipant> participants) {
    return participants.size() == 2
        && (participants.get(0).isWithdrawn() || participants.get(1).isWithdrawn());
  }

  private User resolveOpponent(List<ChatroomParticipant> participants, User user) {
    User first = participants.get(0).getParticipant();
    User second = participants.get(1).getParticipant();
    return first.getId().equals(user.getId()) ? second : first;
  }

  private GetChatResponseDto buildWithdrawnUserChatroomResponse(
      List<GetChatMessageResponseDto> messages,
      User user
  ) {
    messages.forEach(dto -> applyReadStatus(dto, user));
    return GetChatResponseDto.of(GetChatroomMemberInfoResponseDto.withdrawn(), messages);
  }

  private GetChatroomMemberInfoResponseDto buildOpponentMemberInfo(User opponent, List<String> hashtags) {
    return GetChatroomMemberInfoResponseDto.from(opponent, hashtags);
  }

  private void logParticipantCheckResult(Long chatroomId, Long userId, List<Long> participantIds) {
    int size = participantIds.size();
    if (size == 0) {
      log.error("chatroomId={}, userId={}, step=전원_탈퇴_채팅방", chatroomId, userId);
    } else if (size == 1) {
      log.info("chatroomId={}, userId={}, remainingParticipantId={}, step=1인_채팅방",
          chatroomId, userId, participantIds.get(0));
    } else {
      log.info("chatroomId={}, userId={}, participantIds={}", chatroomId, userId, participantIds);
    }
  }

  private record OpponentProfile(String nickname, String imageUrl) {}
}
