package com.lingo.lingoproject.matching.application;

import com.lingo.lingoproject.shared.domain.model.Matching;
import com.lingo.lingoproject.shared.domain.model.MatchingStatus;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.MatchingRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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

  public boolean existsByRequestUserAndRequestedUserAndMatchingStatus(
      User requestUser, User requestedUser, MatchingStatus status) {
    return matchingRepository.existsByRequestUserAndRequestedUserAndMatchingStatus(
        requestUser, requestedUser, status);
  }

  public Matching findFirstByRequestUserAndRequestedUser(User requestUser, User requestedUser) {
    return matchingRepository.findFirstByRequestUserAndRequestedUser(requestUser, requestedUser);
  }
}
