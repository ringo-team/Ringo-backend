package com.lingo.lingoproject.shared.infrastructure.persistence.impl;

import com.lingo.lingoproject.shared.domain.model.QPhotographerImage;
import com.lingo.lingoproject.shared.domain.model.QPhotographerInfo;
import com.lingo.lingoproject.shared.domain.model.QProfile;
import com.lingo.lingoproject.shared.infrastructure.persistence.PhotographerInfoRepositoryCustom;
import com.lingo.lingoproject.snap.presentation.dto.GetPhotographerInfosResponseDto;
import com.lingo.lingoproject.snap.presentation.dto.GetPhotographerInfosResponseDto.GetImageInfoRequestDto;
import com.querydsl.core.group.GroupBy;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class PhotographerInfoRepositoryImpl implements PhotographerInfoRepositoryCustom {
  private final JPAQueryFactory jpaQueryFactory;
  private final QPhotographerInfo qPhotographerInfo = QPhotographerInfo.photographerInfo;
  private final QPhotographerImage qPhotographerImage = QPhotographerImage.photographerImage;
  private final QProfile  qProfile = QProfile.profile;

  public List<GetPhotographerInfosResponseDto> getPhotographerInfos(){
    Map<Long, GetPhotographerInfosResponseDto> result = jpaQueryFactory
        .from(qPhotographerInfo)
        .leftJoin(qPhotographerImage)
        .on(qPhotographerInfo.photographer.id.eq(qPhotographerImage.photographer.id))
        .leftJoin(qProfile)
        .on(qPhotographerInfo.photographer.id.eq(qProfile.user.id))
        .transform(GroupBy.groupBy(qPhotographerInfo.photographer.id).as(
            Projections.constructor(GetPhotographerInfosResponseDto.class,
                qPhotographerInfo.photographer.id,
                qProfile.imageUrl,
                qPhotographerInfo.content,
                qPhotographerInfo.instagramId,
                GroupBy.list(Projections.constructor(GetImageInfoRequestDto.class,
                    qPhotographerImage.imageUrl,
                    qPhotographerImage.snapLocation,
                    qPhotographerImage.snapDate)))
        ));
    return new ArrayList<>(result.values());
  }
}
