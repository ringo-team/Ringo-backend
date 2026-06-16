package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.matching.domain.event.UserScrapEvent;
import com.lingo.lingoproject.matching.presentation.dto.GetScrappedUserResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.shared.domain.event.DomainEventPublisher;
import com.lingo.lingoproject.shared.domain.model.FaceVerify;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.ScrapStatus;
import com.lingo.lingoproject.shared.domain.model.ScrappedUser;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserScrapLog;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.infrastructure.persistence.ScrappedUserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserScrapLogRepository;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.RedisKey;
import com.lingo.lingoproject.shared.utils.RedisUtils;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingScrapUseCase {

  private final ScrappedUserRepository scrappedUserRepository;
  private final UserQueryUseCase userQueryUseCase;
  private final DomainEventPublisher domainEventPublisher;
  private final UserScrapLogRepository userScrapLogRepository;


  @Transactional
  public void scrapUser(Long recommendedUserId, User user) {
    User recommendedUser = userQueryUseCase.유저_찾기_혹은_오류(recommendedUserId);

    UserScrapLog scraplog = UserScrapLog.builder()
        .user(user)
        .scrappedUser(recommendedUser)
        .build();

    if (scrappedUserRepository.existsByUserAndScrappedUser(user, recommendedUser)) {
      scraplog.setStatus(ScrapStatus.CANCEL);
      scrappedUserRepository.deleteByUserAndScrappedUser(user, recommendedUser);
      userScrapLogRepository.save(scraplog);
      return;
    }
    scraplog.setStatus(ScrapStatus.SCRAP);
    scrappedUserRepository.save(ScrappedUser.of(user, recommendedUser));
    userScrapLogRepository.save(scraplog);
    domainEventPublisher.publish(new UserScrapEvent(user, recommendedUser));
  }

  public List<GetScrappedUserResponseDto> getScrappedUsers(User user) {
    return scrappedUserRepository.findAllByUser(user)
        .stream()
        .map(ScrappedUser::getScrappedUser)
        .map(scrappedUser -> {
          Profile profile = scrappedUser.getProfile();
          return new GetScrappedUserResponseDto(
              scrappedUser.getId(),
              scrappedUser.getNickname(),
              LocalDate.now().getYear() - scrappedUser.getBirthday().getYear(),
              profile != null ? profile.getImageUrl() : null,
              profile != null ? (FaceVerify.PASS == profile.getFaceVerify() ? 1 : 0) : 0
          );
        })
        .toList();
  }
}
