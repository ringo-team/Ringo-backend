package com.lingo.lingoproject.user.application;

import com.lingo.lingoproject.shared.domain.model.BlockedFriend;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.infrastructure.persistence.BlockedFriendRepository;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BlockedFriendUseCase {

  private final BlockedFriendRepository blockedFriendRepository;

  public BlockedFriendUseCase(BlockedFriendRepository blockedFriendRepository) {
    this.blockedFriendRepository = blockedFriendRepository;
  }

  public void blockFriend(User user, List<String> phoneNumber){
    List<BlockedFriend> blocked = phoneNumber.stream()
        .map(n -> n.replaceAll("[^0-9]", ""))
        .map(n -> BlockedFriend.of(user, n))
        .toList();
    blockedFriendRepository.saveAll(blocked);
  }
}
