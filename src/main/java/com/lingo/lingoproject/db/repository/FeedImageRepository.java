package com.lingo.lingoproject.db.repository;

import com.lingo.lingoproject.db.domain.FeedImage;
import com.lingo.lingoproject.db.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedImageRepository extends JpaRepository<FeedImage, Long> {

  List<FeedImage> findAllByUser(User user);

  void deleteAllByUser(User user);
}
