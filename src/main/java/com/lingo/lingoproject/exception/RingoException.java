package com.lingo.lingoproject.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RingoException extends RuntimeException{

  private HttpStatus status;
  private ErrorCode errorCode;

  public RingoException(String message, ErrorCode errorCode, HttpStatus status){
    super(message);
    this.status = status;
    this.errorCode = errorCode;
  }
  public RingoException(String message, Throwable cause){
    super(message, cause);
  }
}
