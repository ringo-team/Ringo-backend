package com.lingo.lingoproject.match;

import com.lingo.lingoproject.domain.Matching;
import com.lingo.lingoproject.match.dto.GetUserProfileListResponseDto;
import com.lingo.lingoproject.match.dto.GetUserProfileResponseDto;
import com.lingo.lingoproject.match.dto.MatchingRequestDto;
import com.lingo.lingoproject.match.dto.RequestMatchingResponseDto;
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
    List<GetUserProfileResponseDto> requestUserIds = matchService.getUserIdRequests(id);
    return ResponseEntity.ok().body(new GetUserProfileListResponseDto(requestUserIds));
  }

  @GetMapping("/{id}/requests") // 내가 지목한 사람
  public ResponseEntity<?> getUserRequestedById(@PathVariable("id") Long id){
    List<GetUserProfileResponseDto> requestedUserIds = matchService.getUserIdRequested(id);
    return ResponseEntity.ok().body(new GetUserProfileListResponseDto(requestedUserIds));
  }

  @GetMapping("/{id}/recommend")
  public ResponseEntity<?>  recommend(@PathVariable Long id) {
    List<GetUserProfileResponseDto> rtnList = matchService.recommend(id);
    return ResponseEntity.ok().body(new GetUserProfileListResponseDto(rtnList));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<?> deleteMatching(@PathVariable("id") Long id){
    matchService.deleteMatching(id);
    log.info("성공적으로 매칭 삭제");
    return ResponseEntity.ok().body("성공적으로 매칭을 삭제하였습니다. ");
  }

}
