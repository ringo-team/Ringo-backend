package com.lingo.lingoproject.shared.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 프로젝트 전용 비즈니스 예외 클래스.
 *
 * <h2>사용 방법</h2>
 * <pre>
 * throw new RingoException("유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER);
 * </pre>
 *
 * <h2>예외 처리 흐름</h2>
 * <ul>
 *   <li><b>컨트롤러 계층</b>: {@link RingoExceptionController}(@RestControllerAdvice)가 잡아서
 *       {@link ErrorResponse}를 JSON으로 반환합니다.</li>
 *   <li><b>필터 계층</b>: Spring MVC의 예외 핸들러가 작동하지 않으므로
 *       {@link ExceptionHandlerFilter}가 직접 잡아서 처리합니다.</li>
 *   <li><b>WebSocket 계층</b>: STOMP 인터셉터에서 발생하면 연결이 종료됩니다.</li>
 * </ul>
 *
 * <h2>ErrorCode와 HttpStatus</h2>
 * <p>{@link ErrorCode}는 클라이언트가 에러 종류를 판별하는 애플리케이션 레벨 코드이고,
 * {@link HttpStatus}는 HTTP 프로토콜 레벨의 상태 코드입니다.
 * 두 값을 함께 전달해야 올바른 응답을 생성할 수 있습니다.</p>
 */
@Getter
public class RingoException extends RuntimeException{

  /** HTTP 응답 상태 코드. */
  private ErrorCode errorCode;


  /**
   * 표준 비즈니스 예외 생성자.
   *
   * @param message   사람이 읽을 수 있는 에러 메시지
   * @param errorCode 클라이언트용 에러 코드 (예: ErrorCode.NOT_FOUND_USER)
   */
  public RingoException(String message, ErrorCode errorCode){
    super(message);
    this.errorCode = errorCode;
  }

  /**
   * 원인 예외를 포함하는 생성자. 주로 외부 라이브러리 예외를 래핑할 때 사용합니다.
   *
   * @param message 에러 메시지
   * @param cause   원인 예외
   */
  public RingoException(String message, Throwable cause){
    super(message, cause);
  }

  public HttpStatus getHttpStatus() {
    if (errorCode == null) return HttpStatus.INTERNAL_SERVER_ERROR;
    return switch (errorCode) {
      case BAD_REQUEST, BAD_PARAMETER, NOT_FOUND, ADMIN_NOT_FOUND,
           USER_NOT_FOUND, PROFILE_DUPLICATED, INADEQUATE, FACE_NOT_FOUND, OVERFLOW -> HttpStatus.BAD_REQUEST;
      case FORBIDDEN, BLOCKED, BEFORE_SIGNUP, LOGOUT, NO_AUTH,
           NOT_ADULT, TOKEN_INVALID, TOKEN_EXPIRED -> HttpStatus.FORBIDDEN;
      case UNMODERATE -> HttpStatus.NOT_ACCEPTABLE;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }
}
