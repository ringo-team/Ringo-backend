package com.lingo.lingoproject.match;

import com.lingo.lingoproject.match.dto.MatchingRequestDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/match")
public class MatchController {
  @PostMapping("/request")
  public ResponseEntity<?> requestMatch(@RequestBody MatchingRequestDto matchingRequestDto) {
    return ResponseEntity.ok().build();
  }
  @PatchMapping()
  public ResponseEntity<?>  decideMatch(@RequestParam(value = "decision") String decision,
      @RequestBody MatchingRequestDto matchingRequestDto) {
    return ResponseEntity.ok().build();
  }
  @GetMapping("/recommend")
  public ResponseEntity<?>  recommend(@RequestBody MatchingRequestDto matchingRequestDto) {
    return ResponseEntity.ok().build();
  }

}
