package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.shared.domain.model.UserActivityLog;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserActivityLogRepository extends JpaRepository<UserActivityLog, Long> {

  Collection<UserActivityLog> findAllByStartAfter(LocalDateTime startAfter);

  @Query("select l from UserActivityLog l where l.userId = :userId and l.createdAt > :createAtAfter")
  List<UserActivityLog> findByUserAndCreateAtAfter(
      @Param("userId") Long userId,
      @Param("createAtAfter") LocalDateTime createAtAfter
  );

  @Query("select l from UserActivityLog l where cast(l.start as localdate) > :start and cast(l.start as localdate) < :end")
  List<UserActivityLog> 해당_기간_사이의_유저_활동_로그_조회(
      @Param("start") LocalDate start,
      @Param("end") LocalDate end
  );

  @Query("select distinct l.userId from UserActivityLog l where cast(l.start as localdate) = :date")
  Set<Long> 해당_날짜에_접속한_유저수(LocalDate date);

  @Query(value = """
    select avg(A.total) from
    (select 
      sum(l.activity_minute_duration) as total
    from user_activity_log l
    where date(l.start) = :date 
    group by l.user_id
    ) A
""", nativeQuery = true)
  Double 해당_날짜의_세션_기간_평균_조회(@Param("date") LocalDate date);
}
