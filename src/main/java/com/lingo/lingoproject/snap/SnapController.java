package com.lingo.lingoproject.snap;

import com.lingo.lingoproject.image.ImageService;
import com.lingo.lingoproject.image.dto.GetImageUrlResponseDto;
import com.lingo.lingoproject.snap.dto.ApplySnapShootingRequestDto;
import com.lingo.lingoproject.snap.dto.GetPhotographerInfosRequestDto;
import com.lingo.lingoproject.snap.dto.UpdatePhotographerExampleImagesInfoRequestDto;
import com.lingo.lingoproject.snap.dto.SavePhotographerInfoRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class SnapController {

  private final SnapService snapService;
  private final ImageService imageService;

  @Operation(summary = "스냅 사진 신청")
  @PostMapping("/snaps")
  public ResponseEntity<String> applySnapShooting(@Valid @RequestBody ApplySnapShootingRequestDto dto){
    snapService.applySnapShooting(dto);
    return ResponseEntity.ok().body("성공적으로 촬영 날짜가 저장되었습니다.");
  }

  @Operation(summary = "사진 작가 정보 저장")
  @PostMapping("/photo/snaps")
  public ResponseEntity<String> savePhotographerInfo(@Valid @RequestBody SavePhotographerInfoRequestDto dto){
    snapService.savePhotographerInfo(dto);
    return ResponseEntity.ok().body("성공적으로 작가 정보가 저장되었습니다.");
  }

  @Operation(summary = "촬영 예시 사진 업로드")
  @PostMapping(value = "photo/snaps/{photographerId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<JsonListWrapper<GetImageUrlResponseDto>> uploadPhotographerExampleImages(
      @Parameter(description = "이미지 파일들 업로드")
      @RequestParam(value = "images") List<MultipartFile> images,

      @Parameter(description = "사진 작가 id", example = "103")
      @PathVariable("photographerId") Long photographerId
  ){
    List<GetImageUrlResponseDto> dtos = imageService.uploadPhotographerExampleImages(images, photographerId);
    return ResponseEntity.status(HttpStatus.CREATED).body(new JsonListWrapper<>(dtos));
  }

  @Operation(summary = "촬영 예시 사진 정보 저장")
  @PatchMapping(value = "/photo/snaps")
  public ResponseEntity<String> updatePhotographerExampleImagesInfo(
    @Valid @RequestBody UpdatePhotographerExampleImagesInfoRequestDto dto
  ){
    snapService.updatePhotographerExampleImagesInfo(dto);
    return ResponseEntity.ok().body("정상적으로 사진 정보가 저장되었습니다.");
  }

  @Operation(summary = "작가 정보 가져오기")
  @GetMapping("/snaps/photographers")
  public ResponseEntity<JsonListWrapper<GetPhotographerInfosRequestDto>> getPhotographerInfos(){
    List<GetPhotographerInfosRequestDto> list = snapService.getPhotographerInfos();
    return ResponseEntity.ok().body(new JsonListWrapper<>(list));
  }
}
