package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.PhotographerImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotographerImageRepository extends JpaRepository<PhotographerImage, Long> {

}
