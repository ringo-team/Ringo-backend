package com.lingo.lingoproject.shared.domain.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

@Entity
@Getter
@Table(
    name = "KEYWORDS",
    indexes = {
        @Index(
            name = "idx_keywords_keyword",
            columnList = "keyword"
        )
    }
)
public class Keyword {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String keyword;

  private int score;
}
