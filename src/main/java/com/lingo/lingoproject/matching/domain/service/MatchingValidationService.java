package com.lingo.lingoproject.matching.domain.service;

import com.lingo.lingoproject.shared.domain.model.Matching;
import com.lingo.lingoproject.shared.domain.model.MatchingStatus;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.MatchingRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 매칭 도메인의 비즈니스 규칙을 담당하는 Domain Service.
 *
 * <p>매칭 수락·거절·삭제·메시지 작성 시 적용되는 권한 검증 규칙을 캡슐화한다.
 * 인프라 의존 없이 순수한 도메인 불변 조건만 다룬다.</p>
 *
 * <h2>권한 규칙</h2>
 * <ul>
 *   <li>수락·거절: 피요청자(requestedUser)만 가능</li>
 *   <li>삭제·메시지 조회: 요청자 또는 피요청자 모두 가능</li>
 *   <li>메시지 작성: 요청자(requestUser)만 가능</li>
 * </ul>
 */
@Slf4j
@Service
public class MatchingValidationService {

  private final MatchingRepository matchingRepository;

  public MatchingValidationService(MatchingRepository matchingRepository) {
    this.matchingRepository = matchingRepository;
  }

  /**
   * 매칭 수락·거절 권한을 검증한다.
   * 피요청자(requestedUser)만 응답할 수 있다.
   *
   * @throws RingoException 권한이 없는 경우
   */
  public void validateMatchingRespondPermission(Matching matching, User user) {
    Long requestedUserId = matching.getRequestedUser().getId();
    if (!requestedUserId.equals(user.getId())) {
      log.error("step=매칭_응답_권한없음, authUserId={}, matchRequestedUserId={}, status=FAILED", user.getId(), requestedUserId);
      throw new RingoException(
          "매칭 수락 여부를 결정할 권한이 없습니다.",
          ErrorCode.NO_AUTH);
    }
  }

  public void validateAlreadyMatched(User requestUser, User requestedUser) {
    if (matchingRepository.existsByRequestUserAndRequestedUser(requestUser, requestedUser) ||
        matchingRepository.existsByRequestUserAndRequestedUser(requestedUser, requestUser)){
      throw new RingoException(
          "이미 매칭된 연결입니다.",
          ErrorCode.BAD_REQUEST);
    }
  }

  /**
   * 매칭 삭제 권한을 검증한다.
   * 요청자(requestUser) 또는 피요청자(requestedUser) 모두 삭제할 수 있다.
   *
   * @throws RingoException 권한이 없는 경우
   */
  public void validateMatchingDeletePermission(Matching matching, User user) {
    Long requestedUserId = matching.getRequestedUser().getId();
    Long requestUserId = matching.getRequestUser().getId();
    if (!(requestedUserId.equals(user.getId()) || requestUserId.equals(user.getId()))) {
      log.error("step=매칭_삭제_권한없음, authUserId={}, matchRequestedUserId={}, status=FAILED",
          user.getId(), requestedUserId);
      throw new RingoException(
          "매칭을 삭제할 권한이 없습니다.",
          ErrorCode.NO_AUTH);
    }
  }

  /**
   * 매칭 요청 메시지 작성 권한을 검증한다.
   * 매칭 요청자(requestUser)만 메시지를 작성할 수 있다.
   *
   * @throws RingoException 권한이 없는 경우
   */
  public void validateMatchingMessageWritePermission(Matching matching, User user) {
    Long requestUserId = matching.getRequestUser().getId();
    if (!requestUserId.equals(user.getId())) {
      log.error("step=매칭_메시지_작성_권한없음, authUserId={}, matchRequestUserId={}, status=FAILED", user.getId(), requestUserId);
      throw new RingoException(
          "요청 메세지를 저장 및 수정할 권한이 없습니다.",
          ErrorCode.NO_AUTH);
    }
  }

  public void validateIsAlreadyDecidedMatching(Matching matching){
    if (matching.getMatchingStatus().equals(MatchingStatus.ACCEPTED) ||
        matching.getMatchingStatus().equals(MatchingStatus.REJECTED)) {
      throw new RingoException(
          "이미 매칭된 연결입니다.",
          ErrorCode.BAD_REQUEST);
    }
  }

  /**
   * 매칭 요청 메시지 조회 권한을 검증한다.
   * 요청자(requestUser) 또는 피요청자(requestedUser) 모두 조회할 수 있다.
   *
   * @throws RingoException 권한이 없는 경우
   */
  public void validateMatchingMessageReadPermission(Matching matching, User user) {
    Long requestedUserId = matching.getRequestedUser().getId();
    Long requestUserId   = matching.getRequestUser().getId();
    if (!(requestedUserId.equals(user.getId()) || requestUserId.equals(user.getId()))) {
      log.error("step=매칭_메시지_조회_권한없음, authUserId={}, status=FAILED", user.getId());
      throw new RingoException(
          "해당 매칭의 요청 메세지를 확인할 권한이 없습니다.",
          ErrorCode.NO_AUTH);
    }
  }
}