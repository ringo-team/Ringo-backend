package com.lingo.lingoproject.admin.presentation;

import com.lingo.lingoproject.admin.application.ProfileInspectUseCase;
import com.lingo.lingoproject.admin.presentation.dto.PostProfileReviewRequestDto;
import com.lingo.lingoproject.admin.presentation.dto.ProfileReviewListResponseDto;
import com.lingo.lingoproject.admin.presentation.dto.ProfileReviewResponseDto;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "profile-inspect-conroller", description = "프로필 검수 컨트롤러")
public class ProfileInspectController {

  private final ProfileInspectUseCase profileInspectUseCase;

  @GetMapping(value = "/api/profile-reviews")
  public ResponseEntity<?> getProfileReviews(
      @RequestParam(value = "status") String status,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size
  ){
    ProfileReviewListResponseDto response = profileInspectUseCase.getProfileReviews(status, page, size);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @PostMapping(value = "/api/profile-reviews/{id}/decision")
  public ResponseEntity<?> postProfileReviews(
      @PathVariable(value = "id") Long id,
      @RequestBody PostProfileReviewRequestDto request
  ){
    profileInspectUseCase.postProfileReview(id, request);
    return ResponseEntity.status(HttpStatus.OK).body("success");
  }
}
