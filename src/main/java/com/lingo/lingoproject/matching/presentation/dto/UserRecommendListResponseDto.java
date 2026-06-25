package com.lingo.lingoproject.matching.presentation.dto;

import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import java.util.List;

public class UserRecommendListResponseDto<T> extends ApiListResponseDto<T> {
  public UserRecommendListResponseDto(String result, boolean complete, List<T> t){
    super(result, t);
    this.isProfileVerified = complete;
  }
  boolean isProfileVerified;
}
