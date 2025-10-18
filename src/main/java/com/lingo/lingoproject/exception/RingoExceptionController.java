package com.lingo.lingoproject.exception;

import com.lingo.lingoproject.domain.ExceptionMessage;
import com.lingo.lingoproject.repository.ExceptionMessageRepository;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@RequiredArgsConstructor
@Hidden
public class RingoExceptionController {

  private final ExceptionMessageRepository exceptionMessageRepository;

  @ExceptionHandler(RingoException.class)
  public ResponseEntity<String> handleRingoException(RingoException e){
    exceptionMessageRepository.save(new ExceptionMessage(e.getMessage()));
    return ResponseEntity.status(e.getStatus()).body(e.getMessage());
  }
}
