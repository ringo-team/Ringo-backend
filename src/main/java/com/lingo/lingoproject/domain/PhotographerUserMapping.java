package com.lingo.lingoproject.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "PHOTOGRAPHER_USER_MAPPINGS")
public class PhotographerUserMapping {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "photographer_id")
  private User photographer;

  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;
}
