package com.lingo.lingoproject.survey;

import com.lingo.lingoproject.domain.Survey;
import com.lingo.lingoproject.repository.SurveyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SurveyService {

  private final SurveyRepository surveyRepository;

  public List<GetSurveyResponseDto> getSurveyList(){
    return surveyRepository.findAll()
        .stream()
        .map(s -> new GetSurveyResponseDto(s.getId(), s.getSurveyNum(), s.getContent()))
        .toList();
  }

  public void updateSurvey(UpdateSurveyRequestDto dto){
    Survey survey = surveyRepository.findById(dto.id())
        .orElseThrow(() -> new IllegalArgumentException("설문지 id가 잘못되었습니다."));
    survey.setContent(dto.content());
    surveyRepository.save(survey);
    log.info("설문을 정상적으로 수정하였습니다.");
  }

  public void deleteSurvey(Long id){
    surveyRepository.deleteById(id);
    log.info("id {} 설문이 정상적으로 삭제되었습니다.", id);
  }

}
