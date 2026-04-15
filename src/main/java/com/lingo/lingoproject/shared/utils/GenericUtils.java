package com.lingo.lingoproject.shared.utils;

import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import java.util.Arrays;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;

@Slf4j
public class GenericUtils {

  public static <T> T validateAndReturnEnumValue(T[] array, String value) {
    for (T item : array) {
      if (item.toString().equals(value)) {
        log.info("step=ENUM_값_매핑, enum={}, value={}", item, value);
        return item;
      }
    }
    log.error("step=ENUM_값_매핑_실패, enumList={}, value={}", Arrays.toString(array), value);
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
