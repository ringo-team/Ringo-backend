package com.lingo.lingoproject.survey;

import com.lingo.lingoproject.domain.Survey;
import com.lingo.lingoproject.domain.enums.SurveyCategory;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.SurveyRepository;
import com.lingo.lingoproject.survey.dto.UpdateSurveyRequestDto;
import com.lingo.lingoproject.utils.GenericUtils;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SurveyService {

  private final SurveyRepository surveyRepository;
  private final GenericUtils genericUtils;

  public void uploadSurveyExcel(MultipartFile file){
    List<Survey> surveyList = new ArrayList<>();
    try(InputStream is = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(is)){
      Row firstRow = workbook.getSheetAt(0).getRow(0);
      Sheet sheet = workbook.getSheetAt(0);
      sheet.removeRow(firstRow);
      for(Row row : sheet){
        String categoryValue = row.getCell(2).getStringCellValue();
        if (categoryValue.isEmpty()) break;
        SurveyCategory category = getSurveyCategory(categoryValue);
        Survey survey = Survey.builder()
            .surveyNum((int) (row.getCell(0).getNumericCellValue()))
            .confrontSurveyNum((int) (row.getCell(1).getNumericCellValue()))
            .category(category)
            .content(row.getCell(3).getStringCellValue())
            .purpose(row.getCell(4).getStringCellValue())
            .build();
        surveyList.add(survey);
      }
    }
    catch (RingoException e){
      throw new RingoException(e.getMessage(), e.getStatus());
    }
    catch (Exception e){
      throw new RingoException(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }
    surveyRepository.saveAll(surveyList);
  }

  private static SurveyCategory getSurveyCategory(String categoryValue) {
    System.out.println(categoryValue);
    SurveyCategory category = null;
    if (categoryValue.contains("공간 취향")){
      category = SurveyCategory.SPACE;
    }
    else if (categoryValue.contains("자기 표현")){{
      category = SurveyCategory.SELF_REPRESENTATION;
    }}
    else if (categoryValue.contains("취향 공유")){
      category = SurveyCategory.SHARING;
    }
    else if (categoryValue.contains("컨텐츠 취향")){
      category = SurveyCategory.CONTENT;
    }
    else{
      throw new RingoException("적절하지 않은 카테고리가 입력되었습니다.", HttpStatus.BAD_REQUEST);
    }
    return category;
  }

  public void updateSurvey(UpdateSurveyRequestDto dto){
    Survey survey = surveyRepository.findById(dto.surveyId())
        .orElseThrow(() -> new RingoException("해당 설문을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    if(dto.purpose() != null && !dto.purpose().isEmpty()) survey.setPurpose(dto.purpose());
    if(dto.content() != null && !dto.content().isEmpty()) survey.setContent(dto.content());
    if(dto.category() != null && genericUtils.isContains(SurveyCategory.values(),dto.category()))
      survey.setCategory(SurveyCategory.valueOf(dto.category()));
    surveyRepository.save(survey);
  }

  public List<String> getSurveys(){
    return surveyRepository.findAll()
        .stream()
        .map(Survey::getContent)
        .toList();
  }
}
