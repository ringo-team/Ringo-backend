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
    log.info(e.getMessage());
    exceptionMessageRepository.save(new ExceptionMessage(e.getMessage()));
    return ResponseEntity.status(e.getStatus()).body(new ResultMessageResponseDto(e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ResultMessageResponseDto> handleValidationException(MethodArgumentNotValidException e){
    log.info(e.getMessage());
    return  ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ResultMessageResponseDto("적절한 형식의 요청을 해주시길 바랍니다."));
  }
}
