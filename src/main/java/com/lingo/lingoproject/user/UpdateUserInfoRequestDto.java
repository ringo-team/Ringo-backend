package com.lingo.lingoproject.user;


public record UpdateUserInfoRequestDto(
    Integer height,
  Boolean isDrinking,
  Boolean isSmoking,
  String job,
  String religion,
  String etc
){}