package com.lingo.lingoproject.domain;

import com.lingo.lingoproject.domain.enums.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "PAYMENT_STATUS_LOGS")
public class PaymentStatusLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "payment_id")
  private Payment payment;

  @Column(updatable = false)
  private Integer amount;

  @Column(updatable = false)
  private PaymentStatus paymentStatus;

  @CreationTimestamp
  @Column(updatable = false, nullable = false)
  private LocalDateTime paymentDate;
}
