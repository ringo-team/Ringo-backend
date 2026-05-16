package com.lingo.lingoproject.shared.utils;

import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import java.util.Arrays;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GenericUtils {

  public static <T> T 문자열이_enum에_속하는지_검증후_enum_반환(T[] array, String value) {
    for (T item : array) {
      if (item.toString().equals(value)) {
        log.info("step=ENUM_값_매핑, enum={}, value={}", item, value);
        return item;
      }
    }
    log.error("step=ENUM_값_매핑_실패, enumList={}, value={}", Arrays.toString(array), value);
    throw new RingoException("적절하지 않은 값이 요청되었습니다.", ErrorCode.BAD_PARAMETER);
  }

  public static <E> void enum_검증후_set(
      String property,
      E[] values,
      Consumer<E> setter
      ){
    if (property == null) return;
    setter.accept(문자열이_enum에_속하는지_검증후_enum_반환(values, property));
  }

  public static void 문자열_널_검증_후_set(
      String property,
      Consumer<String> setter
  ){
    if (property == null || property.isBlank()) return;
    setter.accept(property);
  }

}
