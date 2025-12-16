package com.lingo.lingoproject.exception;

public enum ErrorCode {
  SUCCESS("0000"),
  BAD_REQUEST("E0001"),
  FORBIDDEN("E0002"),
  NO_AUTH("E0003"),
  NOT_FOUND("E0004"),
  NOT_FOUND_USER("E0005"),
  NOT_FOUND_ADMIN("E0006"),
  BAD_PARAMETER("E0007"),
  DUPLICATED("E0008"),
  BEFORE_SIGNUP("E0009"),
  FACE_NOT_FOUND("E0010"),
  UNMODERATE("EOO11"),
  OVERFLOW("E0012"),
  INADEQUATE("E0013"),
  BLOCKED("E0014"),
  NOT_ADULT("E0015"),
  LOGOUT("E0016"),
  TOKEN_EXPIRED("E0017"),
  TOKEN_INVALID("E0018"),
  INTERNAL_SERVER_ERROR("E1000");

  private final String code;

  ErrorCode(String code){
    this.code = code;
  }
  public String getCode() {
    return code;
  }
}
