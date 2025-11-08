package com.lingo.lingoproject.repository.impl;

import com.lingo.lingoproject.domain.QPhotographerImage;
import com.lingo.lingoproject.domain.QPhotographerInfo;
import com.lingo.lingoproject.domain.QProfile;
import com.lingo.lingoproject.snap.dto.GetPhotographerInfosRequestDto;
import com.lingo.lingoproject.snap.dto.GetPhotographerInfosRequestDto.GetImageInfoRequestDto;
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
public class PhotographerInfoRepositoryImpl {
  private final JPAQueryFactory jpaQueryFactory;
  private final QPhotographerInfo qPhotographerInfo = QPhotographerInfo.photographerInfo;
  private final QPhotographerImage qPhotographerImage = QPhotographerImage.photographerImage;
  private final QProfile  qProfile = QProfile.profile;

  public List<GetPhotographerInfosRequestDto> getPhotographerInfos(){
    Map<Long, GetPhotographerInfosRequestDto> result = jpaQueryFactory
        .from(qPhotographerInfo)
        .leftJoin(qPhotographerImage)
        .on(qPhotographerInfo.photographer.id.eq(qPhotographerImage.photographer.id))
        .leftJoin(qProfile)
        .on(qPhotographerInfo.photographer.id.eq(qProfile.user.id))
        .transform(GroupBy.groupBy(qPhotographerInfo.photographer.id).as(
            Projections.constructor(GetPhotographerInfosRequestDto.class,
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
