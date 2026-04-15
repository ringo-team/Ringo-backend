package com.lingo.lingoproject.shared.domain.model;

import com.lingo.lingoproject.shared.utils.Timestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Table(name = "EXCEPTION_MESSAGES")
@Entity
@NoArgsConstructor
public class ExceptionMessage extends Timestamp {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Lob
  private String message;

  @Column(length = 30)
  private String errorCode;

  public ExceptionMessage(String message, String errorCode){
    this.message = message;
    this.errorCode = errorCode;
  }
}
