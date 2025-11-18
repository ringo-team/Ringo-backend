package com.lingo.lingoproject.repository.impl;

import com.lingo.lingoproject.domain.QUser;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Gender;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl {
  private final QUser qUser =  QUser.user;
  private final JPAQueryFactory jpaQueryFactory;
  public List<User> findRandomOppositeGenderAndNotBannedFriend(Gender gender, List<Long> banIds){

    BooleanBuilder builder = new BooleanBuilder();

    builder.and(qUser.gender.ne(gender));
    builder.and(qUser.id.notIn(banIds));

    return jpaQueryFactory.selectFrom(qUser)
        .where(builder)
        .orderBy(Expressions.numberTemplate(Double.class, "function('rand')").asc())
        .fetch();

  }
}
