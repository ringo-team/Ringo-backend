package com.lingo.lingoproject.utils;

import org.springframework.stereotype.Component;

@Component
public class GenericUtils {

  public <T> boolean isContains(T[] array, String value) {
    for (T item : array) {
      if (item.toString().equals(value)) {
        return true;
      }
    }
    return false;
  }

}
