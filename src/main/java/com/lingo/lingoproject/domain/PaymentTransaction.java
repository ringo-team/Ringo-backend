package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.PaymentMethod;
import com.lingo.lingoproject.domain.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "payment_transactions")
public class PaymentTransaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String transactionId;

  private int amount;
  private PaymentStatus paymentStatus;
  private PaymentMethod paymentMethod;

  @CreationTimestamp
  @Column(updatable = false, nullable = false)
  private LocalDateTime paymentDate;
}
