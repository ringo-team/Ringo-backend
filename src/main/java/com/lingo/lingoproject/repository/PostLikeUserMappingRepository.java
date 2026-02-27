package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.PostLikeUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostLikeUserMappingRepository extends JpaRepository<PostLikeUserMapping, Long> {

}
