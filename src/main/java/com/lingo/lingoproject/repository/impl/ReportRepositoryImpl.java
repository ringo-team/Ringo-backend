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

  public List<GetReportInfoResponseDto> findReportInfo(GetReportInfoRequestDto dto){

    BooleanBuilder builder = new BooleanBuilder();

    if(dto.userId() == null){
      return null;
    }
    builder.and(report.id.eq(dto.userId()));

    // where 문 기간 옵션
    if(validate(dto.startedAt()) && validate(dto.finishedAt())){
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

    // where 문 신고 상태 옵션
    if(validate(dto.reportedUserStatus())
    ){
      ReportStatus status = GenericUtils.validateAndReturnEnumValue(ReportStatus.values(), dto.reportedUserStatus());
      builder.and(report.reportedUserStatus.eq(status));
    }

    // where 문 신고 강도 옵션
    if (validate(dto.reportIntensity())
    ){
      ReportIntensity intensity = GenericUtils.validateAndReturnEnumValue(ReportIntensity.values(), dto.reportIntensity());
      builder.and(report.reportIntensity.eq(intensity));
    }

    // where 문 신고 정렬 옵션
    OrderSpecifier<?> ordering = null;
    if (validate(dto.ordering())
    ){
      ReportOrdering reportOrdering = GenericUtils.validateAndReturnEnumValue(ReportOrdering.values(), dto.ordering());
      ordering = switch (reportOrdering) {
        case ReportOrdering.CREATED_AT_ASC -> report.createdAt.asc();
        case ReportOrdering.CREATED_AT_DESC -> report.createdAt.desc();
        case ReportOrdering.INTENSITY_ASC -> report.reportIntensity.asc();
        case ReportOrdering.INTENSITY_DESC -> report.reportIntensity.desc();
        case ReportOrdering.STATUS_ASC -> report.reportedUserStatus.asc();
        default -> report.reportedUserStatus.desc();
      };
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

  public boolean validate(String property){
    if (property == null || property.isBlank()) return false;
    return true;
  }

}
