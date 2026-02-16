package com.lingo.lingoproject.exception;

import com.lingo.lingoproject.domain.ExceptionMessage;
import com.lingo.lingoproject.repository.ExceptionMessageRepository;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
@Hidden
public class RingoExceptionController {

  private final ExceptionMessageRepository exceptionMessageRepository;

  @ExceptionHandler(RingoException.class)
  public ResponseEntity<ResultMessageResponseDto> handleRingoException(RingoException e){
    if (e.getStatus().equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
      log.error(e.getMessage(), e);
    } else {
      log.warn(e.getMessage(), e);
    }
    exceptionMessageRepository.save(new ExceptionMessage(e.getMessage(), e.getStatus().toString()));
    return ResponseEntity.status(e.getStatus()).body(new ResultMessageResponseDto(e.getErrorCode().getCode(), "오류가 발생했습니다."));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ResultMessageResponseDto> handleLeftException(Exception e){
    log.error(e.getMessage(), e);
    exceptionMessageRepository.save(new ExceptionMessage(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.toString()));
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ResultMessageResponseDto(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), "오류가 발생했습니다."));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ResultMessageResponseDto> handleValidationException(MethodArgumentNotValidException e){
    log.error(e.getMessage(), e);
    return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto(ErrorCode.BAD_PARAMETER.getCode(), "파라미터 오류"));
  }
}
