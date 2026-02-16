package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Table(name = "EXCEPTION_MESSAGES")
@Entity
@NoArgsConstructor
public class ExceptionMessage extends Timestamp {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String message;

  private String errorCode;

  public ExceptionMessage(String message, String errorCode){
    this.message = message;
    this.errorCode = errorCode;
  }
}
