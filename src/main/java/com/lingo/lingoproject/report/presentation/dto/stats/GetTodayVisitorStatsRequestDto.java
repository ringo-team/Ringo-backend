package com.lingo.lingoproject.report.presentation.dto.stats;

public record GetTodayVisitorStatsRequestDto(
    long numberOfVisitor,
    float ratioOfMaleVisitor,
    float ratioOfFemaleVisitor
) {

}
