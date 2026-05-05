package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.matching.presentation.dto.GetScrappedUserResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.shared.domain.model.FaceVerify;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.ScrappedUser;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.infrastructure.persistence.ScrappedUserRepository;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingScrapUseCase {

  private final MatchingRecommendationUseCase matchingRecommendationUseCase;
  private final ScrappedUserRepository scrappedUserRepository;
  private final UserQueryUseCase userQueryUseCase;

  @Transactional
  public void scrapUser(Long recommendedUserId, User user) {
    Long userId = user.getId();

    List<Long> cumulativeRecommendedIds = extractIdFromProfileDtos(
        matchingRecommendationUseCase.getCumulativeCachedProfile(user));
    log.info("step=스크랩_누적설문_추천목록, recommendedIds={}", cumulativeRecommendedIds);

    List<Long> dailyRecommendedIds = extractIdFromProfileDtos(
        matchingRecommendationUseCase.getDailyCachedProfile(user));
    log.info("step=스크랩_일일설문_추천목록, recommendedIds={}", dailyRecommendedIds);

    List<Long> allRecommendedIds = new ArrayList<>(cumulativeRecommendedIds);
    allRecommendedIds.addAll(dailyRecommendedIds);

    if (!allRecommendedIds.contains(recommendedUserId)) {
      log.info("step=스크랩_대상아님, requestUserId={}, targetUserId={}, recommendedIds={}",
          userId, recommendedUserId, allRecommendedIds);
      return;
    }

    User recommendedUser = userQueryUseCase.findUserOrThrow(recommendedUserId);

    if (scrappedUserRepository.existsByUserAndScrappedUser(user, recommendedUser)) {
      scrappedUserRepository.deleteByUserAndScrappedUser(user, recommendedUser);
      return;
    }
    scrappedUserRepository.save(ScrappedUser.of(user, recommendedUser));
  }

  public List<GetScrappedUserResponseDto> getScrappedUsers(User user) {
    return scrappedUserRepository.findAllByUser(user)
        .stream()
        .map(ScrappedUser::getUser)
        .map(scrappedUser -> {
          Profile profile = scrappedUser.getProfile();
          return new GetScrappedUserResponseDto(
              user.getId(),
              user.getNickname(),
              LocalDate.now().getYear() - user.getBirthday().getYear(),
              profile.getImageUrl(),
              FaceVerify.PASS == profile.getFaceVerify() ? 1 : 0
          );
        })
        .toList();
  }

  private List<Long> extractIdFromProfileDtos(List<GetUserProfileResponseDto> profiles) {
    return Optional.ofNullable(profiles)
        .orElseGet(Collections::emptyList)
        .stream()
        .map(GetUserProfileResponseDto::getUserId)
        .toList();
  }
}
