package com.lingo.lingoproject.match;

import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.match.dto.GetMatchingRequestMessageResponseDto;
import com.lingo.lingoproject.match.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.match.dto.MatchingRequestDto;
import com.lingo.lingoproject.match.dto.RequestMatchingResponseDto;
import com.lingo.lingoproject.match.dto.SaveMatchingRequestMessageRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchController {

  private final MatchService matchService;

  @Operation(
      summary = "매칭요청",
      description = "유저가 매칭 요청을 할 때 사용하는 api"
  )
  @PostMapping()
  public ResponseEntity<RequestMatchingResponseDto> requestMatching(@RequestBody MatchingRequestDto matchingRequestDto) {
    Matching matching = matchService.matchRequest(matchingRequestDto);
    return ResponseEntity.ok().body(new RequestMatchingResponseDto(matching.getId()));
  }

  @Operation(
      summary = "매칭 응답",
      description = "매칭 요청에 대한 응답(승낙, 거부)을 하는 api"
  )
  @PatchMapping()
  public ResponseEntity<String>  responseToMatching(
      @Parameter(
          description = "매칭 승낙 여부",
          example = "ACCEPTED",
          schema = @Schema(allowableValues = {"ACCEPTED", "REJECTED"})
      )
      @NotBlank @RequestParam(value = "decision") String decision,
      @NotNull @RequestParam(value = "matchingId") Long matchingId) {
    matchService.responseToRequest(decision, matchingId);
    return ResponseEntity.ok().body("매칭 상태가 변경되었습니다.");
  }

  @Operation(summary = "나에게 매칭 요청한 사람 확인")
  @GetMapping("/{id}/requested") // 나를 지목한 사람
  public ResponseEntity<JsonListWrapper<GetUserProfileResponseDto>> getUserWhoRequestsToId(
      @Parameter(description = "유저id", example = "5")
      @PathVariable("id") Long id){
    List<GetUserProfileResponseDto> requestUserIds = matchService.getUserIdRequests(id);
    return ResponseEntity.ok().body(new JsonListWrapper<>(requestUserIds));
  }

  @Operation(summary = "내가 매칭 요청한 사람 확인")
  @GetMapping("/{id}/requests") // 내가 지목한 사람
  public ResponseEntity<JsonListWrapper<GetUserProfileResponseDto>> getUserRequestedById(
      @Parameter(description = "유저id", example = "5")
      @PathVariable("id") Long id){
    List<GetUserProfileResponseDto> requestedUserIds = matchService.getUserIdRequested(id);
    return ResponseEntity.ok().body(new JsonListWrapper<>(requestedUserIds));
  }

  @Operation(
      summary = "이성 친구 추천",
      description = "70% 확률로 매칭이 가능한 이성을 무작위로 골라 추천"
  )
  @GetMapping("/{id}/recommend")
  public ResponseEntity<JsonListWrapper<GetUserProfileResponseDto>>  recommend(
      @Parameter(description = "유저id", example = "5")
      @PathVariable Long id) {
    List<GetUserProfileResponseDto> rtnList = matchService.recommend(id);
    return ResponseEntity.ok().body(new JsonListWrapper<>(rtnList));
  }

  @Operation(summary = "매칭 삭제")
  @DeleteMapping("/{id}")
  public ResponseEntity<String> deleteMatching(
      @Parameter(description = "매칭 id", example = "5")
      @PathVariable("id") Long id){
    matchService.deleteMatching(id);
    log.info("성공적으로 매칭 삭제");
    return ResponseEntity.ok().body("성공적으로 매칭을 삭제하였습니다.");
  }

  @Operation(summary = "매칭 요청 매세지 저장")
  @PostMapping("/message")
  public ResponseEntity<String> saveMatchingRequestMessage(@RequestBody
      SaveMatchingRequestMessageRequestDto dto){
    matchService.saveMatchingRequestMessage(dto);
    return ResponseEntity.ok().body("매칭 메세지가 성공적으로 저장되었습니다.");
  }

  @Operation(summary = "매칭 요청 메세지 조회")
  @GetMapping("/message")
  public ResponseEntity<GetMatchingRequestMessageResponseDto> getMatchingRequestMessage(@NotNull @RequestParam(value = "matchingId") Long matchingId){
    String message = matchService.getMatchingRequestMessage(matchingId);
    return ResponseEntity.ok().body(new GetMatchingRequestMessageResponseDto(message));
  }

}
