package com.lingo.lingoproject.utils;

import java.util.function.Consumer;
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

  public <E extends Enum<E>> void validateAndSetEnum(
      String property,
      E[] values,
      Consumer<E> setter,
      Class<E> clazz){
    if (property == null) return;
    if (!isContains(values, property)) return;
    setter.accept(E.valueOf(clazz, property));
  }

  public void validateAndSetStringValue(
      String property,
      Consumer<String> setter
  ){
    if (property == null || property.isBlank()) return;
    setter.accept(property);
  }

}
