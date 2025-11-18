package com.lingo.lingoproject.stats.dto;

public record GetDailyNumberOfVisitorRequestDto(
    String visitDate,
    Integer numberOfVisitor,
    Integer numberOfSignupUser
) {

}
