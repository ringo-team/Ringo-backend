package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.report.presentation.dto.GetReportInfoRequestDto;
import com.lingo.lingoproject.report.presentation.dto.GetReportInfoResponseDto;
import java.util.List;

public interface ReportRepositoryCustom {

  List<GetReportInfoResponseDto> findReportInfo(GetReportInfoRequestDto dto);
}
