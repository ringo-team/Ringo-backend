package com.lingo.lingoproject.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class RingoException extends RuntimeException{

  private HttpStatus status;

  public RingoException(String message, HttpStatus status){
    super(message);
    this.status = status;
  }
  public RingoException(String message, Throwable cause){
    super(message,cause);
  }
}
