package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.matching.domain.event.MatchingAcceptedEvent;
import com.lingo.lingoproject.matching.domain.event.MatchingRejectedEvent;
import com.lingo.lingoproject.matching.domain.event.MatchingRequestedEvent;
import com.lingo.lingoproject.matching.domain.service.MatchingValidationService;
import com.lingo.lingoproject.matching.domain.service.SurveyScoreCalculator;
import com.lingo.lingoproject.matching.presentation.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.matching.presentation.dto.MatchingRequestDto;
import com.lingo.lingoproject.matching.presentation.dto.SaveMatchingRequestMessageRequestDto;
import com.lingo.lingoproject.shared.domain.event.DomainEventPublisher;
import com.lingo.lingoproject.shared.domain.model.Hashtag;
import com.lingo.lingoproject.shared.domain.model.Matching;
import com.lingo.lingoproject.shared.domain.model.MatchingStatus;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserMatchingLog;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.HashtagRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ProfileRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserMatchingLogRepository;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingRequestUseCase {

  private final MatchQueryUseCase matchQueryUseCase;
  private final UserQueryUseCase userQueryUseCase;
  private final MatchingValidationService matchingValidationService;
  private final SurveyScoreCalculator surveyScoreCalculator;
  private final DomainEventPublisher eventPublisher;
  private final UserMatchingLogRepository userMatchingLogRepository;
  private final ProfileRepository profileRepository;
  private final HashtagRepository hashtagRepository;

  @Transactional
  public Matching requestMatching(MatchingRequestDto dto) {
    User requestUser = userQueryUseCase.findUserOrThrow(dto.requestId());
    User requestedUser = userQueryUseCase.findUserOrThrow(dto.requestedId());

    if (matchQueryUseCase.existsByRequestUserAndRequestedUser(requestUser, requestedUser) ||
        matchQueryUseCase.existsByRequestUserAndRequestedUser(requestedUser, requestUser)) {
      throw new RingoException("이미 매칭된 연결입니다.", ErrorCode.BAD_REQUEST);
    }

    float surveyScore = surveyScoreCalculator.calculate(requestedUser.getId(), requestUser.getId());

    Matching matching = Matching.of(
        requestUser,
        requestedUser,
        surveyScore,
        MatchingStatus.PENDING,
        dto.message()
    );

    log.info("step=매칭_요청, requestUserId={}, requestedUserId={}, surveyScore={}",
        requestUser.getId(), requestedUser.getId(), surveyScore);

    Matching saved = matchQueryUseCase.save(matching);
    userMatchingLogRepository.save(saved.createMatchingLog());

    eventPublisher.publish(new MatchingRequestedEvent(
        saved.getId(),
        requestUser.getId(),
        requestedUser.getId()
    ));
    return saved;
  }

  @Transactional
  public void respondToMatchingRequest(String decision, Long matchingId, User user) {
    Matching matching = matchQueryUseCase.findMatchingOrThrow(matchingId);

    matchingValidationService.validateMatchingRespondPermission(matching, user);
    matchingValidationService.validateIsAlreadyDecidedMatching(matching);

    switch (decision) {
      case "ACCEPTED" -> acceptMatching(matching);
      case "REJECTED" -> rejectMatching(matching);
      default -> throw new RingoException("decision 값이 적절하지 않습니다.", ErrorCode.BAD_PARAMETER);
    }

    log.info("step=매칭_응답, requestedUserId={}, requestUserId={}, matchingStatus={}, matchingScore={}",
        user.getId(), matching.getRequestUser().getId(),
        matching.getMatchingStatus(), matching.getMatchingScore());

    userMatchingLogRepository.save(matching.createRespondLog(user));
    matchQueryUseCase.save(matching);
  }

  @Transactional
  public void deleteMatching(Long matchingId, User user) {
    Matching match = matchQueryUseCase.findMatchingOrThrow(matchingId);

    log.info("step=매칭_삭제_요청, matchingId={}, requestorId={}", matchingId, user.getId());
    matchingValidationService.validateMatchingDeletePermission(match, user);
    matchQueryUseCase.deleteById(matchingId);
  }

  @Transactional
  public void saveMatchingRequestMessage(
      SaveMatchingRequestMessageRequestDto dto,
      Long matchingId,
      User user
  ) {
    Matching matching = matchQueryUseCase.findMatchingOrThrow(matchingId);

    matchingValidationService.validateMatchingMessageWritePermission(matching, user);
    matchingValidationService.validateIsAlreadyDecidedMatching(matching);

    matching.updateRequestMessage(dto.message());

    log.info("step=매칭_요청_메세지_저장, matchingId={}, requestUserId={}, matchingStatus={}, requestUserGender={}",
        matching.getId(), matching.getRequestUser().getId(),
        matching.getMatchingStatus(), matching.getRequestUser().getGender());

    matchQueryUseCase.save(matching);
    userMatchingLogRepository.save(matching.createMatchingLog());
  }

  public String getMatchingRequestMessage(Long matchingId, User user) {
    Matching match = matchQueryUseCase.findMatchingOrThrow(matchingId);
    matchingValidationService.validateMatchingMessageReadPermission(match, user);
    return match.getMatchingRequestMessage();
  }

  public List<GetUserProfileResponseDto> getSentMatchingProfiles(User requestUser) {
    List<Long> matchingIds = matchQueryUseCase.findMatchingIdsByRequestUserExcludingPreRequested(requestUser);
    List<GetUserProfileResponseDto> profiles =
        profileRepository.getRequestedUserProfilesByMatchingIds(matchingIds);
    enrichProfilesWithUserInfo(profiles);
    return profiles;
  }

  public List<GetUserProfileResponseDto> getReceivedMatchingProfiles(User requestedUser) {
    List<Long> matchingIds = matchQueryUseCase.findMatchingIdsByRequestedUserExcludingPreRequested(requestedUser);
    List<GetUserProfileResponseDto> profiles =
        profileRepository.getRequestUserProfilesByMatchingIds(matchingIds);
    enrichProfilesWithUserInfo(profiles);
    return profiles;
  }

  private void acceptMatching(Matching matching) {
    matching.accept();
    matchQueryUseCase.save(matching);
    eventPublisher.publish(new MatchingAcceptedEvent(
        matching.getId(),
        matching.getRequestUser().getId(),
        matching.getRequestedUser().getId()
    ));
  }

  private void rejectMatching(Matching matching) {
    matching.reject();
    matchQueryUseCase.save(matching);
    eventPublisher.publish(new MatchingRejectedEvent(
        matching.getId(),
        matching.getRequestUser().getId(),
        matching.getRequestedUser().getId()
    ));
  }

  private void enrichProfilesWithUserInfo(List<GetUserProfileResponseDto> profiles) {
    List<Long> userIds = profiles.stream().map(GetUserProfileResponseDto::getUserId).toList();
    Map<Long, User> userMap = userQueryUseCase.findAllByIdIn(userIds).stream()
        .collect(Collectors.toMap(User::getId, Function.identity()));
    Map<Long, List<String>> hashtagsMap = batchGetHashtagsByUsers(new ArrayList<>(userMap.values()));

    profiles.forEach(profile -> {
      User user = userMap.get(profile.getUserId());
      profile.setHashtags(hashtagsMap.getOrDefault(user.getId(), List.of()));
      profile.setAge(LocalDate.now().getYear() - user.getBirthday().getYear());

      log.info("step=매칭_프로필_조회, userId={}, age={}, gender={}, nickname={}, matchingScore={}, matchingStatus={}, hashtag={}, faceVerified={}, daysFromLastAccess={}, mbti={}",
          profile.getUserId(), profile.getAge(), profile.getGender(),
          profile.getNickname(), profile.getMatchingScore(), profile.getMatchingStatus(),
          profile.getHashtags(), profile.getVerify(), profile.getDaysFromLastAccess(),
          profile.getMbti());
    });
  }

  private Map<Long, List<String>> batchGetHashtagsByUsers(List<User> users) {
    return hashtagRepository.findAllByUserIn(users).stream()
        .collect(Collectors.groupingBy(
            h -> h.getUser().getId(),
            Collectors.mapping(Hashtag::getHashtag, Collectors.toList())
        ));
  }
}
