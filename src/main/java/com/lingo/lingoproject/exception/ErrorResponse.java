package com.lingo.lingoproject.exception;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ErrorResponse {

  @JsonIgnore
  private final ObjectMapper objectMapper = new ObjectMapper();

  private final String timestamp;
  private final int status;
  private final String message;

  public static ErrorResponse of(String timestamp, int status, String message){
    return new ErrorResponse(timestamp, status, message);
  }

  public ErrorResponse(String timestamp, int status, String message) {
    this.timestamp = timestamp;
    this.status = status;
    this.message = message;
  }

  public String convertToJson() throws JsonProcessingException {
    return objectMapper.writeValueAsString(this);
  }
}
