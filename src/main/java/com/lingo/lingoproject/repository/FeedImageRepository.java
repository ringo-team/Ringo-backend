package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.FeedImage;
import com.lingo.lingoproject.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

  List<FeedImage> findAllByUser(User user);

  void deleteAllByUser(User user);
}
