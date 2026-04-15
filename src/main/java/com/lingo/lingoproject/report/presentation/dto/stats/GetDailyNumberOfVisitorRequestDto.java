package com.lingo.lingoproject.report.presentation.dto.stats;

public record GetDailyNumberOfVisitorRequestDto(
    String visitDate,
    Integer numberOfVisitor,
    Integer numberOfSignupUser
) {

}
