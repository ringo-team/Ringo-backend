package com.lingo.lingoproject.api.stats.dto;

public record GetTodayVisitorStatsRequestDto(
    long numberOfVisitor,
    float ratioOfMaleVisitor,
    float ratioOfFemaleVisitor
) {

}
