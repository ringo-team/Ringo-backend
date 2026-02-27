package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.CommentLikeUserMapping;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentLikeUserMappingRepository extends JpaRepository<CommentLikeUserMapping, Long> {

}
