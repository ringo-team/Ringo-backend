package com.lingo.lingoproject.repository;

import com.lingo.lingoproject.domain.BlockedUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BlockUsersRepository extends JpaRepository<BlockedUser, Long> {

}
