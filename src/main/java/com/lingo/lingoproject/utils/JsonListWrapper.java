package com.lingo.lingoproject.utils;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JsonListWrapper<T> {
  private List<T> list;

  public JsonListWrapper(List<T> list) {
    this.list = list;
  }
}
