package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.Profile;
import com.lingo.lingoproject.domain.SnapImage;
import com.lingo.lingoproject.domain.User;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SnapImageRepository extends JpaRepository<SnapImage, Long> {

  List<SnapImage> findAllByUser(User user);

  void deleteAllByUser(User user);
}
