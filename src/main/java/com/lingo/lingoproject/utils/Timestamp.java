package com.lingo.lingoproject.utils;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class Timestamp {
  @CreationTimestamp
  @Column(updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(insertable = false)
  LocalDateTime updatedAt;
}
