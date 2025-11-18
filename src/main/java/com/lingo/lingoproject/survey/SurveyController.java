package com.lingo.lingoproject.survey;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.Role;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.survey.dto.GetSurveyRequestDto;
import com.lingo.lingoproject.survey.dto.UpdateSurveyRequestDto;
import com.lingo.lingoproject.survey.dto.UploadSurveyRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
  public ResponseEntity<String> updateSurvey(@RequestBody UpdateSurveyRequestDto dto, @AuthenticationPrincipal
      User user){
    if(!user.getRole().equals(Role.ADMIN)){
      throw new RingoException("관리자만 설문지를 수정할 수 있습니다.", HttpStatus.BAD_REQUEST);
    }
    surveyService.updateSurvey(dto);
    return ResponseEntity.status(HttpStatus.OK).body("성공적으로 수정하였습니다.");
  }

  @Operation(summary = "설문지 조회")
  @GetMapping()
  public ResponseEntity<JsonListWrapper<GetSurveyRequestDto>> getSurveys(){
    List<GetSurveyRequestDto> list = surveyService.getSurveys();
    return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(list));
  }

  @Operation(summary = "설문지 응답 저장", description = "유저가 진행한 설문지 응답 저장")
  @PostMapping("/answer")
  public ResponseEntity<String> saveSurveyResponse(@RequestBody JsonListWrapper<UploadSurveyRequestDto> responses,
      @AuthenticationPrincipal User user){
    surveyService.saveSurveyResponse(responses, user);
    return ResponseEntity.ok().body("정상적으로 설문지 응답이 저장되었습니다.");
  }

  @Operation(summary = "일일 설문 조회", description = "날마다 진행하는 설문 문항들 조회, 만약 설문을 진행했으면 null 반환")
  @GetMapping("/daily")
  public ResponseEntity<JsonListWrapper<GetSurveyRequestDto>> getDailySurveys(@RequestParam Long userId){
    List<GetSurveyRequestDto> dto = surveyService.getDailySurveys(userId);
    return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(dto));
  }
}
