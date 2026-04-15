package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.snap.presentation.dto.GetPhotographerInfosResponseDto;
import java.util.List;

public interface PhotographerInfoRepositoryCustom {

  List<GetPhotographerInfosResponseDto> getPhotographerInfos();
}
