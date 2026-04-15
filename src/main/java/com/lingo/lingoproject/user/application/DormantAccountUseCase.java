package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.shared.domain.model.DormantAccount;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserAccessLog;
import com.lingo.lingoproject.shared.infrastructure.persistence.DormantAccountRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserAccessLogRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 휴면 계정 관리 및 유저 접속 로그 Use Case
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DormantAccountUseCase {

  private final DormantAccountRepository dormantAccountRepository;
  private final UserAccessLogRepository userAccessLogRepository;

  public void updateDormantAccount(User user) {
    if (dormantAccountRepository.existsByUser(user)) {
      dormantAccountRepository.deleteByUser(user);
      return;
    }
    dormantAccountRepository.save(DormantAccount.of(user));
  }

  public void saveUserAccessLog(User user) {
    if (userAccessLogRepository.existsByUserIdAndCreateAtAfter(user.getId(), LocalDate.now().atStartOfDay())) {
      return;
    }
    int userAge = LocalDate.now().getYear() - user.getBirthday().getYear();
    log.info("user가 앱을 실행하였습니다. user-id: {}, user-age: {}, user-nickname: {}, user-gender: {}",
        user.getId(), userAge, user.getNickname(), user.getGender());
    userAccessLogRepository.save(UserAccessLog.of(user, userAge));
  }
}