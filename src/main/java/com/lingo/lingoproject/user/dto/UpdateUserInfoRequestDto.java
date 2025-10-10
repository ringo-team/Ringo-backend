package com.lingo.lingoproject.user.dto;


public record UpdateUserInfoRequestDto(
    String height,
  Boolean isDrinking,
  Boolean isSmoking,
  String job,
  String religion,
  String etc
){}