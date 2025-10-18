package com.lingo.lingoproject.survey;
import com.lingo.lingoproject.survey.dto.UpdateSurveyRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/surveys")
@RequiredArgsConstructor
public class SurveyController {

  private final SurveyService surveyService;

  @Operation(summary = "설문지 업로드")
  @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<String> uploadSurveyExcel(@RequestParam("file")MultipartFile file){
    surveyService.uploadSurveyExcel(file);
    return ResponseEntity.status(HttpStatus.CREATED).body("성공적으로 업로드 되었습니다.");
  }

  @Operation(summary = "설문지 수정")
  @PatchMapping()
  public ResponseEntity<String> updateSurvey(@RequestBody UpdateSurveyRequestDto dto){
    surveyService.updateSurvey(dto);
    return ResponseEntity.status(HttpStatus.OK).body("성공적으로 수정하였습니다.");
  }

  @Operation(summary = "설문지 조회")
  @GetMapping()
  public ResponseEntity<JsonListWrapper<String>> getSurveys(){
    List<String> list = surveyService.getSurveys();
    return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(list));
  }
}
