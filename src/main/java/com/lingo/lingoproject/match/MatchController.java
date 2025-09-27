package com.lingo.lingoproject.match;

import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.match.dto.MatchingRequestDto;
import com.lingo.lingoproject.match.dto.RequestMatchingResponseDto;
import com.lingo.lingoproject.match.dto.RequestUserResponseDto;
import com.lingo.lingoproject.match.dto.RequestedUserResponseDto;
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

  @PostMapping()
  public ResponseEntity<?> requestMatching(@RequestBody MatchingRequestDto matchingRequestDto) {
    Matching matching = matchService.matchRequest(matchingRequestDto);
    return ResponseEntity.ok().body(new RequestMatchingResponseDto(matching.getId()));
  }
  @PatchMapping()
  public ResponseEntity<?>  responseToMatching(@RequestParam(value = "decision") String decision,
      @RequestParam(value = "matchingId") Long matchingId) {
    matchService.responseToRequest(decision, matchingId);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{id}/requested") // 나를 지목한 사람
  public ResponseEntity<?> getUserWhoRequestsToId(@PathVariable("id") Long id){
    List<Long> requestUserIds = matchService.getUserIdRequests(id);
    return ResponseEntity.ok().body(new RequestUserResponseDto(requestUserIds));
  }

  @GetMapping("/{id}/requests") // 내가 지목한 사람
  public ResponseEntity<?> getUserRequestedById(@PathVariable("id") Long id){
    List<Long> requestedUserIds = matchService.getUserIdRequested(id);
    return ResponseEntity.ok().body(new RequestedUserResponseDto(requestedUserIds));
  }

  @GetMapping("/recommend")
  public ResponseEntity<?>  recommend(@RequestBody MatchingRequestDto matchingRequestDto) {
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteMatching(@PathVariable("id") Long id){
    matchService.deleteMatching(id);
    log.info("성공적으로 매칭 삭제");
    return ResponseEntity.ok().body("성공적으로 매칭을 삭제하였습니다. ");
  }

}
