package com.lingo.lingoproject.stats.dto;

public record GetTodayVisitorStatsRequestDto(
    long numberOfVisitor,
    float ratioOfMaleVisitor,
    float ratioOfFemaleVisitor
) {

}
