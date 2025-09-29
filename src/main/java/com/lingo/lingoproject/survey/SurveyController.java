package com.lingo.lingoproject.survey;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/surveys")
@RequiredArgsConstructor
public class SurveyController {

  private final SurveyService surveyService;

  @GetMapping()
  public ResponseEntity<?> getSurveyList(){
    List<GetSurveyResponseDto> rtnList = surveyService.getSurveyList();
    return ResponseEntity.ok().body(rtnList);
  }

  @PatchMapping
  public ResponseEntity<?> updateSurvey(@RequestBody UpdateSurveyRequestDto updateSurveyRequestDto){
    surveyService.updateSurvey(updateSurveyRequestDto);
    return ResponseEntity.ok().body("설문이 정상적으로 수정되었습니다.");
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteSurvey(@PathVariable Long id){
    surveyService.deleteSurvey(id);
    return ResponseEntity.ok().body("해당 설문이 정상적으로 삭제되었습니다.");
  }
}
