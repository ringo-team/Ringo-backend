package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.shared.domain.model.BlockedUser;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 유저 차단(블락) Use Case
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockUserUseCase {

  private final UserQueryUseCase userQueryUseCase;
  private final BlockedUserRepository blockedUserRepository;
  private final ApplicationEventPublisher eventPublisher;

  @Transactional
  public void blockUser(Long userId, Long adminId) {
    User admin = userQueryUseCase.findById(adminId)
        .orElseThrow(() -> new RingoException("id 에 해당하는 관리자가 없습니다.", ErrorCode.USER_NOT_FOUND));
    User user = userQueryUseCase.findById(userId)
        .orElseThrow(() -> new RingoException("id에 해당하는 유저가 없습니다.", ErrorCode.ADMIN_NOT_FOUND));

    BlockedUser blockedUser = BlockedUser.of(user, admin);

    log.info("admin이 특정 user를 block했습니다. admin-id: {}, block-user-id: {}, block-user-phone-number: {}",
        admin.getId(), user.getId(), user.getPhoneNumber());

    blockedUserRepository.save(blockedUser);
  }
}