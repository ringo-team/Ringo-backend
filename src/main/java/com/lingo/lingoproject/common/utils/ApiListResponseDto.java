package com.lingo.lingoproject.common.utils;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApiListResponseDto<T> {
  private String result;
  private List<T> list;

  public ApiListResponseDto(String result, List<T> list) {
    this.result = result;
    this.list = list;
  }
}
