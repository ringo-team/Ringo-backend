package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.shared.domain.model.Matching;
import com.lingo.lingoproject.shared.domain.model.MatchingStatus;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.MatchingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchQueryUseCase {

  private final MatchingRepository matchingRepository;

  public Matching findMatchingOrThrow(Long matchingId) {
    return matchingRepository.findById(matchingId)
        .orElseThrow(() -> new RingoException("적절하지 않은 매칭 id 입니다.", ErrorCode.NOT_FOUND));
  }

  public Matching save(Matching matching) {
    return matchingRepository.save(matching);
  }

  public void deleteById(Long matchingId) {
    matchingRepository.deleteById(matchingId);
  }

  public List<Long> findMatchingIdsByRequestUserExcludingPreRequested(User user) {
    return matchingRepository.findMatchingIdsByRequestUserExcludingPreRequested(user);
  }

  public List<Long> findMatchingIdsByRequestedUserExcludingPreRequested(User user) {
    return matchingRepository.findMatchingIdsByRequestedUserExcludingPreRequested(user);
  }

  public List<Long> findRequestedUserIdsByRequestUser(User user) {
    return matchingRepository.findRequestedUserIdsByRequestUser(user);
  }

  public List<Long> findRequestUserIdsByRequestedUser(User user) {
    return matchingRepository.findRequestUserIdsByRequestedUser(user);
  }

  public List<Matching> findAllByRequestUserOrRequestedUser(User u1, User u2) {
    return matchingRepository.findAllByRequestUserOrRequestedUser(u1, u2);
  }

  public void deleteAllByRequestedUser(User user) {
    matchingRepository.deleteAllByRequestedUser(user);
  }

  public void deleteAllByRequestUser(User user) {
    matchingRepository.deleteAllByRequestUser(user);
  }

  public boolean existsByRequestUserAndRequestedUser(User requestUser, User requestedUser) {
    return matchingRepository.existsByRequestUserAndRequestedUser(requestUser, requestedUser);
  }

  public boolean 다음_매칭_요청자_응답자_매칭상태가_이미_존재하는지(
      User requestUser, User requestedUser, MatchingStatus status) {
    return matchingRepository.existsByRequestUserAndRequestedUserAndMatchingStatus(
        requestUser, requestedUser, status);
  }

  public Matching findFirstByRequestUserAndRequestedUser(User requestUser, User requestedUser) {
    return matchingRepository.findFirstByRequestUserAndRequestedUser(requestUser, requestedUser);
  }

  public void 매칭된_유저들인지_검증(User user1, User user2) {
    boolean 매칭여부 =
        다음_매칭_요청자_응답자_매칭상태가_이미_존재하는지(
            user1, user2, MatchingStatus.ACCEPTED) ||
            다음_매칭_요청자_응답자_매칭상태가_이미_존재하는지(
                user2, user1, MatchingStatus.ACCEPTED);

    if (!매칭여부) {
      매칭_되지_않은_유저들의_요청_에러_로그_생성(user1, user2);
      throw new RingoException(
          "매칭되지 않은 쌍은 채팅방을 생성할 수 없습니다.",
          ErrorCode.NO_AUTH);
    }
  }

  private void 매칭_되지_않은_유저들의_요청_에러_로그_생성(User user1, User user2) {
    Matching matching1 = findFirstByRequestUserAndRequestedUser(user1, user2);
    Matching matching2 = findFirstByRequestUserAndRequestedUser(user2, user1);

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
}
