package com.lingo.lingoproject.api.stats.dto;

public record GetDailyNumberOfVisitorRequestDto(
    String visitDate,
    Integer numberOfVisitor,
    Integer numberOfSignupUser
) {

}
