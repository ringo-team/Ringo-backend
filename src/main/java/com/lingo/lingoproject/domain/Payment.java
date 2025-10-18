package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.PaymentMethod;
import com.lingo.lingoproject.domain.enums.PaymentStatus;
import com.lingo.lingoproject.utils.Timestamp;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "PAYMENTS")
public class Payment extends Timestamp {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  private PaymentMethod method;
  private PaymentStatus status;
  private int amount;
}
