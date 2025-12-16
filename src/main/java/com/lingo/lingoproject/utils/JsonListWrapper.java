package com.lingo.lingoproject.utils;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class JsonListWrapper<T> {
  private String result;
  private List<T> list;

  public JsonListWrapper(String result, List<T> list) {
    this.result = result;
    this.list = list;
  }
}
