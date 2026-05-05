package com.lingo.lingoproject.shared.exception;

import com.lingo.lingoproject.shared.domain.model.ExceptionMessage;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.infrastructure.persistence.ExceptionMessageRepository;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
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
    if (e.getErrorCode() == null || e.getErrorCode().equals(ErrorCode.INTERNAL_SERVER_ERROR)) {
      log.error(e.getMessage(), e);
    } else {
      log.warn(e.getMessage(), e);
    }
    HttpStatus httpStatus = switch (e.getErrorCode()){
      case ErrorCode.BAD_REQUEST,
           ErrorCode.BAD_PARAMETER,
           ErrorCode.NOT_FOUND,
           ErrorCode.ADMIN_NOT_FOUND,
           ErrorCode.USER_NOT_FOUND,
           ErrorCode.PROFILE_DUPLICATED,
           ErrorCode.INADEQUATE,
           ErrorCode.FACE_NOT_FOUND,
           ErrorCode.OVERFLOW -> HttpStatus.BAD_REQUEST;
      case ErrorCode.FORBIDDEN,
           ErrorCode.BLOCKED ,
           ErrorCode.BEFORE_SIGNUP,
           ErrorCode.LOGOUT,
           ErrorCode.NO_AUTH,
           ErrorCode.NOT_ADULT,
           ErrorCode.TOKEN_INVALID,
           ErrorCode.TOKEN_EXPIRED -> HttpStatus.FORBIDDEN;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
    exceptionMessageRepository.save(new ExceptionMessage(e.getMessage(), httpStatus.toString()));
    return ResponseEntity.status(httpStatus).body(new ResultMessageResponseDto(e.getErrorCode().getCode(), "오류가 발생했습니다."));
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
