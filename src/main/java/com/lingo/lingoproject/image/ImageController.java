package com.lingo.lingoproject.image;

import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.image.dto.GetImageUrlResponseDto;
import com.lingo.lingoproject.image.dto.UpdateSnapImageDescriptionRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping()
@RequiredArgsConstructor
public class ImageController {

  private final ImageService imageService;

  @Operation(
      summary = "프로필 이미지 업로드",
      description = "s3에 이미지를 업로드합니다"
  )
  @ApiResponse(
      responseCode = "201",
      description = "생성 성공"
  )
  @PostMapping(value = "/profiles/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<GetImageUrlResponseDto> uploadProfileImage(
      @Parameter(description = "이미지 파일")
      @RequestParam(value = "image") MultipartFile image,

      @Parameter(description = "유저id", example = "12")
      @PathVariable("userId") Long userId
      ) {

    GetImageUrlResponseDto dto = imageService.uploadProfileImage(image, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(dto);
  }

  @Operation(summary = "스냅 사진 일괄 업로드")
  @PostMapping(value = "/snaps/{userId}", consumes =  MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<JsonListWrapper<GetImageUrlResponseDto>> uploadSnapImages(
      @Parameter(description = "이미지 파일들 업로드")
      @RequestParam(value = "images") List<MultipartFile> images,

      @Parameter(description = "유저 id", example = "5")
      @PathVariable("userId") Long userId
  ){
    List<GetImageUrlResponseDto> dtos = imageService.uploadSnapImages(images, userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(new JsonListWrapper<>(dtos));
  }

  @Operation(
      summary = "프로필 조회.",
      description = "프로필 URL과 이미지 id를 반환합니다."
  )
  @GetMapping("/profiles/{userId}")
  public ResponseEntity<GetImageUrlResponseDto> getProfileImageUrl(
      @Parameter(description = "이미지 id", example = "5")
      @PathVariable(value = "userId") Long userId){
    GetImageUrlResponseDto dto = imageService.getProfileImageUrl(userId);
    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }

  @Operation(summary = "프로필 이미지 업데이트")
  @PatchMapping("profiles/{profileId}")
  public ResponseEntity<GetImageUrlResponseDto> updateProfileImage(
      @Parameter(description = "업데이트할 사진")
      @RequestParam(value = "image") MultipartFile image,

      @Parameter(description = "업데이트 할 프로필 사진의 id")
      @PathVariable(value = "profileId") Long profileId,

      @AuthenticationPrincipal User user
  ){
    GetImageUrlResponseDto dto = imageService.updateProfileImage(image, profileId, user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }

  @Operation(
      summary = "스냅 이미지 조회.",
      description = "유저가 올린 모든 스냅 사진을 조회"
  )
  @GetMapping("/snaps")
  public ResponseEntity<JsonListWrapper<GetImageUrlResponseDto>> getAllSnapImageUrls(
      @Parameter(description = "유저 Id", example = "5")
      @RequestParam("userId") Long userId
  ){
    List<GetImageUrlResponseDto> responses = imageService.getAllSnapImageUrls(userId);
    return ResponseEntity.status(HttpStatus.OK).body(new JsonListWrapper<>(responses));
  }

  @Operation(summary = "스냅 사진 업데이트")
  @PatchMapping("/snaps/{snapImageId}")
  public ResponseEntity<GetImageUrlResponseDto> updateSnapImage(
      @Parameter(description = "업데이트할 사진")
      @RequestParam(value = "image") MultipartFile image,

      @Parameter(description = "업데이트할 스냅 사진의 id")
      @PathVariable(value = "snapImageId") Long snapImageId,

      @Parameter(description = "업데이트한 스냅 사진의 설명")
      @RequestParam(value = "description") String description,

      @AuthenticationPrincipal User user
  ){
    GetImageUrlResponseDto dto = imageService.updateSnapImage(image, snapImageId, description, user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }

  @Operation(
      summary = "프로필 사진 삭제",
      description = "프로필 id에 해당하는 이미지를 삭제합니다."
  )
  @DeleteMapping("/profiles/{profileId}")
  public ResponseEntity<String> deleteProfileImage(
      @Parameter(description = "프로필 id", example = "12")
      @PathVariable(value = "profileId") Long profileId,

      @AuthenticationPrincipal User user
  ){
    imageService.deleteProfile(profileId, user.getId());
    return ResponseEntity.ok().body("이미지를 성공적으로 삭제했습니다.");
  }

  @Operation(
      summary = "스냅 사진 삭제",
      description = "스냅 사진 id에 해당하는 이미지를 삭제합니다."
  )
  @DeleteMapping("/snaps/{snapImageId}")
  public ResponseEntity<String> deleteSnapImage(
    @Parameter(description = "스냅 사진 id", example = "11")
      @PathVariable(value = "snapImageId") Long snapImageId,

      @AuthenticationPrincipal User user
  ){
    imageService.deleteSnapImage(snapImageId, user.getId());
    return ResponseEntity.status(HttpStatus.OK).body("성공적으로 스냅 사진을 삭제하였습니다.");
  }

  @Operation(summary = "스냅 사진 내용 저장")
  @PatchMapping("/snaps")
  public ResponseEntity<String> updateSnapImageDescription(
      @RequestBody UpdateSnapImageDescriptionRequestDto dto
  ){
    imageService.updateSnapImageDescription(dto);
    return ResponseEntity.status(HttpStatus.OK).body("성공적으로 스냅 사진 설명을 성공적으로 저장하였습니다.");
  }
}
