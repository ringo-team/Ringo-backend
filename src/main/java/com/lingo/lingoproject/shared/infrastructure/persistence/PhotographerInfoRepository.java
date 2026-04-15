package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.PhotographerInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotographerInfoRepository extends JpaRepository<PhotographerInfo, Long>, PhotographerInfoRepositoryCustom {

}
