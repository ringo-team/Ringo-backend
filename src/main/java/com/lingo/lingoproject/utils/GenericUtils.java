package com.lingo.lingoproject.utils;

import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import java.util.function.Consumer;
import org.springframework.http.HttpStatus;

public class GenericUtils {

  public static <T> T validateAndReturnEnumValue(T[] array, String value) {
    for (T item : array) {
      if (item.toString().equals(value)) {
        return item;
      }
    }
    throw new RingoException("적절하지 않은 값이 요청되었습니다.", ErrorCode.BAD_PARAMETER, HttpStatus.BAD_REQUEST);
  }

  public static <E> void validateAndSetEnum(
      String property,
      E[] values,
      Consumer<E> setter
      ){
    if (property == null) return;
    setter.accept(validateAndReturnEnumValue(values, property));
  }

  public static void validateAndSetStringValue(
      String property,
      Consumer<String> setter
  ){
    if (property == null || property.isBlank()) return;
    setter.accept(property);
  }

}
