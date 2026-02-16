package com.lingo.lingoproject.image;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.image.dto.FeedImageDataListDto;
import com.lingo.lingoproject.image.dto.GetImageUrlResponseDto;
import com.lingo.lingoproject.image.dto.UpdateFeedImageDescriptionRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "image-controller", description = "프로필, 피드 사진 관련 api")
public interface ImageApi {

  @Operation(summary = "프로필 이미지 업로드", description = "s3에 이미지를 업로드합니다")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "생성 성공", content = @Content(schema = @Schema(implementation = GetImageUrlResponseDto.class))),
      @ApiResponse(responseCode = "E0008", description = "이미 프로필이 업로드 되어 있음", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0005", description = "유저를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0010", description = "프로필에 얼굴이 나오지 않음", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0011", description = "프로필에 부적절한 부분이 검출됨", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부에러, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping(value = "profiles", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<?> uploadProfileImage(
      @Parameter(description = "이미지 파일") @RequestParam(value = "image") MultipartFile image,
      @NotNull Long userId
  );

  @Operation(summary = "프로필 조회", description = "프로필 URL과 이미지 id를 반환합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "조회 성공", content = @Content(schema = @Schema(implementation = GetImageUrlResponseDto.class))),
      @ApiResponse(responseCode = "E0005", description = "해당 id로 유저를 찾을 수 없음", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 에러, 기타문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("/users/{userId}/profile")
  ResponseEntity<GetImageUrlResponseDto> getProfileImageUrl(
      @Parameter(description = "이미지 id", example = "5") @PathVariable(value = "userId") Long userId);

  @Operation(summary = "프로필 이미지 업데이트")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "업데이트 성공", content = @Content(schema = @Schema(implementation = GetImageUrlResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "업데이트할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0010", description = "프로필에 얼굴이 나오지 않음", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0011", description = "프로필에 부적절한 부분이 검출됨", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
  })
  @PatchMapping(value = "profiles/{profileId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<GetImageUrlResponseDto> updateProfileImage(
      @Parameter(description = "업데이트할 사진") @RequestParam(value = "image") MultipartFile image,
      @Parameter(description = "업데이트 할 프로필 사진의 id") @PathVariable(value = "profileId") Long profileId,
      @AuthenticationPrincipal User user
  );

  @Operation(summary = "프로필 사진 삭제", description = "프로필 id에 해당하는 이미지를 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "삭제 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "삭제 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부에러, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @DeleteMapping("/profiles/{profileId}")
  ResponseEntity<ResultMessageResponseDto> deleteProfileImage(
      @Parameter(description = "프로필 id", example = "12") @PathVariable(value = "profileId") Long profileId,
      @AuthenticationPrincipal User user
  );

  @Operation(summary = "피드 사진 일괄 업로드")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "업로드 성공", content = @Content(schema = @Schema(implementation = JsonListWrapper.class))),
      @ApiResponse(responseCode = "E0007", description = "잘못된 유저 id 파라미터", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0012", description = "업로드 사진 개수 초과", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping(value = "/feeds", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<JsonListWrapper<GetImageUrlResponseDto>> uploadFeedImages(
      @Parameter(description = "이미지 파일들 업로드") @ModelAttribute FeedImageDataListDto images,
      @Parameter(description = "유저 id", example = "5") @RequestParam("userId") Long userId

      //@AuthenticationPrincipal User user
  );

  @Operation(summary = "피드 이미지 조회.", description = "유저가 올린 모든 피드 사진을 조회")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "조회 성공", content = @Content(schema = @Schema(implementation = JsonListWrapper.class))),
      @ApiResponse(responseCode = "E0005", description = "id로 유저를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @GetMapping("users/{userId}/feeds")
  ResponseEntity<JsonListWrapper<GetImageUrlResponseDto>> getAllFeedImageUrls(
      @Parameter(description = "유저 Id", example = "5") @PathVariable(value = "userId") Long userId
  );

  @Operation(summary = "피드 사진 업데이트")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "업데이트 성공", content = @Content(schema = @Schema(implementation = GetImageUrlResponseDto.class))),
      @ApiResponse(responseCode = "E0011", description = "부적절한 사진 사용", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0004", description = "해당 id로 피드사진을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "피드사진을 업데이트할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PatchMapping("/feeds/{feedImageId}")
  ResponseEntity<?> updateFeedImage(
      @Parameter(description = "업데이트할 사진") @RequestParam(value = "image") MultipartFile image,
      @Parameter(description = "업데이트할 피드 사진의 id") @PathVariable(value = "feedImageId") Long feedImageId,
      @Parameter(description = "업데이트한 피드 사진의 설명") @RequestParam(value = "description") String description,
      @AuthenticationPrincipal User user
  );

  @Operation(summary = "피드 사진 삭제", description = "피드 사진 id에 해당하는 이미지를 삭제합니다.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "삭제 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0004", description = "id에 해당하는 피드사진을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "삭제 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부에러, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @DeleteMapping("/feeds/{feedImageId}")
  ResponseEntity<ResultMessageResponseDto> deleteFeedImage(
      @Parameter(description = "피드 사진 id", example = "11") @PathVariable(value = "feedImageId") Long feedImageId,
      @AuthenticationPrincipal User user
  );

  @Operation(summary = "피드 사진 설명 저장")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "저장 완료", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0004", description = "해당 id로 피드사진을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "피드사진을 저장할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PatchMapping("/feeds/{feedImageId}/description")
  ResponseEntity<ResultMessageResponseDto> updateFeedImageDescription(
      @Parameter(description = "피드 사진 id", example = "11") @PathVariable(value = "feedImageId") Long feedImageId,
      @RequestBody UpdateFeedImageDescriptionRequestDto dto,
      @AuthenticationPrincipal User user
  );

  @Operation(summary = "얼굴 인증")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "얼굴 인증 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0013", description = "얼굴 인증 실패", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
  })
  @PostMapping(value = "/profiles/verify", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<ResultMessageResponseDto> verifyProfile(
      @RequestParam(value = "image") MultipartFile image,
      @AuthenticationPrincipal User user
  );

}
