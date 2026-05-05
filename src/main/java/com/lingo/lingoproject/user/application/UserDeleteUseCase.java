package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.Withdrawer;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.AnsweredSurveyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedFriendRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ChatroomParticipantRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.DormantAccountRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.FcmTokenRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.MatchingRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserPointRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.WithdrawerRepository;
import com.lingo.lingoproject.image.application.S3ImageStorageService;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 회원 탈퇴 Use Case
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDeleteUseCase {

  private final UserQueryUseCase userQueryUseCase;
  private final WithdrawerRepository withdrawerRepository;
  private final AnsweredSurveyRepository answeredSurveyRepository;
  private final BlockedFriendRepository blockedFriendRepository;
  private final DormantAccountRepository dormantAccountRepository;
  private final S3ImageStorageService imageService;
  private final MatchingRepository matchingRepository;
  private final FcmTokenRepository fcmTokenRepository;
  private final ChatroomParticipantRepository chatroomParticipantRepository;
  private final UserPointRepository userPointRepository;

  @Transactional
  public void deleteUser(User user, String reason, String feedback) {
    try {
      withdrawerRepository.save(Withdrawer.of(ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDate.now()), reason, feedback));

      answeredSurveyRepository.deleteAllByUser(user);
      blockedFriendRepository.deleteByUser(user);
      dormantAccountRepository.deleteByUser(user);
      imageService.deleteProfileImage(user);
      imageService.deleteAllFeedImagesByUser(user);
      matchingRepository.deleteAllByRequestedUser(user);
      matchingRepository.deleteAllByRequestUser(user);
      fcmTokenRepository.deleteByUser(user);
      chatroomParticipantRepository.disconnectChatroomParticipantWithUser(user);
      userPointRepository.deleteAllByUser(user);
      userQueryUseCase.delete(user);
    } catch (Exception e) {
      log.error("유저 데이터 삭제 실패. userId: {}, reason: {}", user.getId(), reason, e);
      throw new RingoException("유저 정보를 삭제하는데 실패하였습니다." + e.getMessage(),
          ErrorCode.INTERNAL_SERVER_ERROR);
    }
  }
}
