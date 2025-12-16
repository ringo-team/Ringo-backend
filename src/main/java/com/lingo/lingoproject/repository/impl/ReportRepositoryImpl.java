package com.lingo.lingoproject.repository.impl;


import com.lingo.lingoproject.domain.QReport;
import com.lingo.lingoproject.domain.enums.ReportIntensity;
import com.lingo.lingoproject.domain.enums.ReportOrdering;
import com.lingo.lingoproject.domain.enums.ReportStatus;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.report.dto.GetReportInfoRequestDto;
import com.lingo.lingoproject.report.dto.GetReportInfoResponseDto;
import com.lingo.lingoproject.report.dto.QGetReportInfoResponseDto;
import com.lingo.lingoproject.utils.GenericUtils;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Repository;

@RequiredArgsConstructor
@Repository
@Slf4j
public class ReportRepositoryImpl{
  private final JPAQueryFactory jpaQueryFactory;

  private final QReport report = QReport.report;
  private final GenericUtils genericUtils;

  public List<GetReportInfoResponseDto> findReportInfo(GetReportInfoRequestDto dto){

    BooleanBuilder builder = new BooleanBuilder();

    if(dto.userId() != null){
      builder.and(report.id.eq(dto.userId()));
    }

    if(dto.startedAt() != null && !dto.startedAt().isBlank()
        && dto.finishedAt() != null && !dto.finishedAt().isBlank() ){
      try {
        LocalDateTime startedAt = LocalDate.parse(dto.startedAt().trim()).atStartOfDay();
        LocalDateTime finishedAt = LocalDate.parse(dto.finishedAt().trim()).atTime(23, 59, 59);

        builder.and(report.createdAt.goe(startedAt));
        builder.and(report.createdAt.loe(finishedAt));
      }catch (Exception e){
        log.error("신고 조회 기간 파싱 실패. startedAt: {}, finishedAt: {}", dto.startedAt(), dto.finishedAt(), e);
        throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
      }
    }

    if(dto.reportedUserStatus() != null &&
        !dto.reportedUserStatus().isEmpty() &&
        genericUtils.isContains(ReportStatus.values(), dto.reportedUserStatus())
    ){
      builder.and(report.reportedUserStatus.eq(ReportStatus.valueOf(dto.reportedUserStatus())));
    }

    if (dto.reportIntensity() != null &&
        !dto.reportIntensity().isEmpty() &&
        genericUtils.isContains(ReportIntensity.values(), dto.reportIntensity())
    ){
      builder.and(report.reportIntensity.eq(ReportIntensity.valueOf(dto.reportIntensity())));
    }

    OrderSpecifier<?> ordering = report.createdAt.asc();
    if (dto.ordering() != null &&
        !dto.ordering().isEmpty() &&
        genericUtils.isContains(ReportOrdering.values(), dto.ordering())
    ){
      if(ReportOrdering.CREATED_AT_DESC.equals(ReportOrdering.valueOf(dto.ordering()))){
        ordering = report.createdAt.desc();
      }
      else if(ReportOrdering.INTENSITY_ASC.equals(ReportOrdering.valueOf(dto.ordering()))){
        ordering = report.reportIntensity.asc();
      }
      else if(ReportOrdering.CREATED_AT_DESC.equals(ReportOrdering.valueOf(dto.ordering()))){
        ordering = report.createdAt.desc();
      }
      else if (ReportOrdering.STATUS_ASC.equals(ReportOrdering.valueOf(dto.ordering()))){
        ordering = report.reportedUserStatus.asc();
      }
      else{
        ordering = report.reportedUserStatus.desc();
      }
    }

    return jpaQueryFactory
        .select(new QGetReportInfoResponseDto(
            report.id,
            report.reportUserId,
            report.reportedUserId,
            report.reportIntensity,
            report.reportedUserStatus
        ))
        .from(report)
        .where(builder)
        .orderBy(ordering)
        .fetch();

  }
}
