package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserPoint;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserPointRepository;
import com.lingo.lingoproject.user.presentation.dto.GetUserPointResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPointUseCase {

  private final UserPointRepository userPointRepository;

  public GetUserPointResponseDto getUserPoints(User user){
    UserPoint point = userPointRepository.findByUser(user);
    return new GetUserPointResponseDto(point.getPoint());
  }
}
