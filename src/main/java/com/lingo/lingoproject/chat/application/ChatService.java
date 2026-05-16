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
import com.lingo.lingoproject.matching.application.MatchQueryUseCase;
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
 * {@link #채팅_접속_유저_매핑_가져오기}는 이 키의 존재 여부로 현재 채팅방에 연결된 사용자를 판별합니다.</p>
 *
 * <h2>탈퇴 사용자 처리</h2>
 * <p>{@link ChatroomParticipant#is회원탈퇴한_유저인지()}이 true인 참여자가 있으면
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

  private final ChatroomRepository chatroomRepository;
  private final MessageRepository messageRepository;
  private final UserQueryUseCase userQueryUseCase;
  private final ChatroomParticipantRepository chatroomParticipantRepository;
  private final MatchQueryUseCase matchQueryUseCase;
  private final RedisTemplate<String, Object> redisTemplate;
  private final FailedChatMessageLogRepository failedChatMessageLogRepository;
  private final AppointmentRepository appointmentRepository;

  // ============================================================
  // Public API — 채팅 메시지
  // ============================================================

  @Transactional
  public GetChatResponseDto getChatMessages(
      User 채팅방에_입장한_유저,
      Long 채팅방_id,
      int page,
      int size
  ) {

    if (page < 0 || size < 1 || size > 100) throw new RingoException("잘못된 페이지 요청입니다.", ErrorCode.BAD_REQUEST);
    if (채팅방_id == null) throw new RingoException("채팅방 ID가 없습니다.", ErrorCode.BAD_REQUEST);

    채팅방에_속한_유저인지_검증_혹은_오류(채팅방_id, 채팅방에_입장한_유저.getId());

    Chatroom 채팅방 = 채팅방_찾기_혹은_에러(채팅방_id);

    List<GetChatMessageResponseDto> 채팅메세지 = 해당_채팅방_메세지_페이지네이션_조회(채팅방_id, page, size);
    messageRepository.모든_메세지_읽음_처리(채팅방_id, 채팅방에_입장한_유저.getId());

    ChatroomParticipant 상대방 = 채팅방.채팅_상대방_유저_조회(채팅방에_입장한_유저);

    ChatOpponentInfoDto 회원탈퇴한_유저_dto = ChatOpponentInfoDto.회원탈퇴한_유저_dto_생성();
    if (상대방.is회원탈퇴한_유저인지()) return GetChatResponseDto.of(회원탈퇴한_유저_dto, 채팅메세지);

    // 로그
    ChatOpponentInfoDto 상대방_유저_dto = ChatOpponentInfoDto.상대방_유저_dto_생성(상대방.getParticipant());
    return GetChatResponseDto.of(상대방_유저_dto, 채팅메세지);
  }

  public Message saveMessage(GetChatMessageResponseDto messageDto, Long chatroomId) {
    return messageRepository.save(Message.of(chatroomId, messageDto));
  }

  public Map<User, Boolean> 채팅_접속_유저_매핑_가져오기(Long 채팅방_id) {
    Chatroom 채팅방 = 채팅방_찾기_혹은_에러(채팅방_id);
    List<User> 회원탈퇴하지_않고_채팅방_참여자인_유저들 = 채팅방.getNonWithdrawnParticipant();
    try {
      Map<User, Boolean> 채팅_접속_유저_매핑 = 회원탈퇴하지_않고_채팅방_참여자인_유저들.stream()
          .collect(Collectors.toMap(
              유저 -> 유저,
              유저 -> Boolean.TRUE.equals(
                  redisTemplate.hasKey("connect::" + 유저.getId() + "::" + 채팅방_id)
              )
          ));

      return 채팅_접속_유저_매핑;

    } catch (Exception e) {
      log.error("step=접속자_조회_실패, chatroomId={}, status=FAILED", 채팅방_id, e);
      throw new RingoException("채팅방 접속자 조회 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }

  public void 메세지_전송_오류시_기록(
      Exception 에러메세지,
      Message 메세지,
      String 발신자_로그인_아이디,
      String 경로
  ) {
    log.error("step=메시지_전송_실패, destination={}, senderLoginId={}, status=FAILED", 경로, 발신자_로그인_아이디, 에러메세지);

    failedChatMessageLogRepository.save(
        FailedChatMessageLog.메세지_전송_오류_로그_생성(
            웹소켓_경로로부터_채팅방_id_추출_없으면_null_반환(경로),
            에러메세지,
            메세지 != null ? 메세지.getId() : null,
            경로,
            발신자_로그인_아이디
        ));
  }

  // ============================================================
  // Public API — 채팅방 관리
  // ============================================================

  public List<GetChatroomResponseDto> getChatroomsByUser(User 유저) {
    List<Chatroom> 유저의_채팅방_목록 = chatroomRepository.findAllByUser(유저);
    List<Long> 채팅방_ids = 채팅방_id_추출(유저의_채팅방_목록);

    Map<Long, ChatroomSummaryProjection> summaryMap = messageRepository
        .findChatroomSummaries(채팅방_ids, 유저.getId())
        .stream()
        .collect(Collectors.toMap(ChatroomSummaryProjection::getChatroomId, p -> p));

    return 유저의_채팅방_목록.stream()
        .filter(채팅방 -> 채팅방.유저가_채팅방을_나갔는지(유저))
        .map(채팅방 -> 채팅_목록_응답_dto_생성(채팅방, 유저, summaryMap.get(채팅방.getId())))
        .toList();
  }

  private GetChatroomResponseDto 채팅_목록_응답_dto_생성(
      Chatroom chatroom,
      User user,
      ChatroomSummaryProjection summary
  ){
    ChatroomParticipant participant = chatroom.채팅_상대방_유저_조회(user);
    User opponent;

    if (participant.is회원탈퇴한_유저인지()) opponent = null;
    else opponent = participant.getParticipant();

    return GetChatroomResponseDto.of(chatroom, opponent, summary);
  }

  private List<Long> 채팅방_id_추출(List<Chatroom> chatrooms){
    return chatrooms.stream().map(Chatroom::getId).toList();
  }

  @Transactional
  public Chatroom 채팅방_생성(CreateChatroomRequestDto dto) {
    ChatType 채팅방_타입 = GenericUtils.문자열이_enum에_속하는지_검증후_enum_반환(ChatType.values(), dto.chatType());

    User 첫번째_유저 = userQueryUseCase.유저_찾기_혹은_오류(dto.user1Id());
    User 두번째_유저 = userQueryUseCase.유저_찾기_혹은_오류(dto.user2Id());

    매칭된_유저들인지_검증(첫번째_유저, 두번째_유저);

    long minId = Math.min(첫번째_유저.getId(), 두번째_유저.getId());
    long maxId = Math.max(첫번째_유저.getId(), 두번째_유저.getId());
    String 채팅방명 = minId + "_" + maxId;

    Chatroom 저장된_채팅방 = chatroomRepository.save(Chatroom.of(채팅방명, 채팅방_타입));
    chatroomParticipantRepository.saveAll(List.of(
        ChatroomParticipant.of(첫번째_유저, 저장된_채팅방),
        ChatroomParticipant.of(두번째_유저, 저장된_채팅방)
    ));

    log.info("""
            step=채팅방_생성,
            user1Id={}, user1Nickname={}, user1Gender={},
            user1Birthday={}, user2Id={}, user2Nickname={},
            user2Gender={}, user2Birthday={}, chatType={}, createdAt={}
            """,
        첫번째_유저.getId(), 첫번째_유저.getNickname(), 첫번째_유저.getGender(), 첫번째_유저.getBirthday(),
        두번째_유저.getId(), 두번째_유저.getNickname(), 두번째_유저.getGender(), 두번째_유저.getBirthday(),
        저장된_채팅방.getType(), 저장된_채팅방.getCreatedDate());

    return 저장된_채팅방;
  }

  @Transactional
  public void 채팅방_삭제(Long chatroomId, User user) {

    채팅방에_속한_유저인지_검증_혹은_오류(chatroomId, user.getId());

    Chatroom chatroom = 채팅방_찾기_혹은_에러(chatroomId);
    chatroomParticipantRepository.deleteAllByChatroom(chatroom);
    messageRepository.deleteAllByChatroomId(chatroomId);
    chatroomRepository.delete(chatroom);
  }

  @Transactional
  public void 채팅방에_속한_유저인지_검증_혹은_오류(Long chatroomId, Long userId) {
    Chatroom chatroom = 채팅방_찾기_혹은_에러(chatroomId);
    User user = userQueryUseCase.유저_찾기_혹은_오류(userId);
    log.info("chatroomId={}, userId={}", chatroomId, userId);
    if (!chatroom.채팅방에_속하는지(user)) {
      log.error("chatroomId={}, userId={}, step=채팅방_접근_권한_없음", chatroomId, userId);
      throw new RingoException("채팅방에 소속되지 않은 유저입니다.", ErrorCode.NO_AUTH);
    }
  }

  // ============================================================
  // Public API — 약속
  // ============================================================

  @Transactional
  public GetChatMessageResponseDto 약속_잡기(User 등록자, SaveAppointmentRequestDto dto) {
    Chatroom chatroom = 채팅방_찾기_혹은_에러(dto.chatroomId());

    채팅방에_속한_유저인지_검증_혹은_오류(dto.chatroomId(), dto.registerId());

    log.info("""
        step=약속_생성,
        chatroomId={}, registrantNickname={}, place={},
        appointmentTime={}, isAlert={}, alertTime={}""",
        chatroom.getId(), 등록자.getNickname(),
        dto.place(), dto.appointmentTime(),
        dto.isAlert() == 1, dto.alertTime());

    Appointment savedAppointment = appointmentRepository.save(Appointment.of(chatroom, 등록자, dto));

    return GetChatMessageResponseDto.약속_등록_메세지_dto_생성(savedAppointment);
  }

  public List<Appointment> 현재_이전에_처리되지_않은_약속_알림_가져오기() {
    return appointmentRepository.findAllByAlertTimeBeforeAndIsAlert(LocalDateTime.now(), true);
  }

  public void 약속_저장(List<Appointment> appointments) {
    appointmentRepository.saveAll(appointments);
  }

  // ============================================================
  // Public API — 사용자 / 유틸
  // ============================================================


  public User findUserByLoginIdOrThrow(String loginId) {
    return userQueryUseCase.findByLoginId(loginId)
        .orElseThrow(() -> new RingoException("유저 정보가 올바르지 않습니다.", ErrorCode.FORBIDDEN));
  }

  /** destination 경로의 마지막 세그먼트에서 채팅방 ID를 추출한다. 예: "/user/queue/topic/42" → 42L */
  public Long 웹소켓_경로로부터_채팅방_id_추출_없으면_null_반환(String destination) {
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

  private Chatroom 채팅방_찾기_혹은_에러(Long chatroomId) {
    return chatroomRepository.findById(chatroomId)
        .orElseThrow(() -> new RingoException("채팅방 찾을 수 없음", ErrorCode.BAD_PARAMETER));
  }

  private void 매칭된_유저들인지_검증(User user1, User user2) {
    boolean 매칭여부 =
        matchQueryUseCase.다음_매칭_요청자_응답자_매칭상태가_이미_존재하는지(
            user1, user2, MatchingStatus.ACCEPTED) ||
        matchQueryUseCase.다음_매칭_요청자_응답자_매칭상태가_이미_존재하는지(
            user2, user1, MatchingStatus.ACCEPTED);

    if (!매칭여부) {
      매칭_되지_않은_유저들의_요청_에러_로그_생성(user1, user2);
      throw new RingoException(
          "매칭되지 않은 쌍은 채팅방을 생성할 수 없습니다.",
          ErrorCode.NO_AUTH);
    }
  }

  private void 매칭_되지_않은_유저들의_요청_에러_로그_생성(User user1, User user2) {
    Matching matching1 = matchQueryUseCase.findFirstByRequestUserAndRequestedUser(user1, user2);
    Matching matching2 = matchQueryUseCase.findFirstByRequestUserAndRequestedUser(user2, user1);

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
  private List<GetChatMessageResponseDto> 해당_채팅방_메세지_페이지네이션_조회(Long chatroomId, int page, int size) {
    Pageable pageable = PageRequest.of(page, size);
    return messageRepository.findAllByChatroomIdOrderByCreatedAtDesc(chatroomId, pageable)
        .stream()
        .map(GetChatMessageResponseDto::from)
        .toList();
  }
}
