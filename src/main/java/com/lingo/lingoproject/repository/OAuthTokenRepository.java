package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.OAuthToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthTokenRepository extends JpaRepository<OAuthToken, Long> {
  Optional<OAuthToken> findByUserToken(Long userToken);
}
