package com.lingo.lingoproject.snap;

import com.lingo.lingoproject.image.ImageService;
import com.lingo.lingoproject.image.dto.GetImageUrlResponseDto;
import com.lingo.lingoproject.snap.dto.ApplySnapShootingRequestDto;
import com.lingo.lingoproject.snap.dto.GetPhotographerInfosRequestDto;
import com.lingo.lingoproject.snap.dto.UpdatePhotographerExampleImagesInfoRequestDto;
import com.lingo.lingoproject.snap.dto.SavePhotographerInfoRequestDto;
import com.lingo.lingoproject.utils.JsonListWrapper;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import com.lingo.lingoproject.exception.RingoException;

@RestController
@Slf4j
@RequiredArgsConstructor
public class SnapController {

  private final SnapService snapService;
  private final ImageService imageService;

  @Operation(summary = "스냅 사진 신청")
  @PostMapping("/snaps")
  public ResponseEntity<ResultMessageResponseDto> applySnapShooting(@Valid @RequestBody ApplySnapShootingRequestDto dto){
    try {
      log.info("step=스냅_촬영_신청_시작, status=SUCCESS");
      snapService.applySnapShooting(dto);
      log.info("step=스냅_촬영_신청_완료, status=SUCCESS");
    } catch (Exception e) {
      log.error("step=스냅_촬영_신청_실패, status=FAILED", e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("스냅 촬영 신청에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return ResponseEntity.ok().body(new ResultMessageResponseDto("성공적으로 촬영 날짜가 저장되었습니다."));
  }

  @Operation(summary = "사진 작가 정보 저장")
  @PostMapping("/photographers/{photographerId}")
  public ResponseEntity<ResultMessageResponseDto> savePhotographerInfo(
      @Valid @RequestBody SavePhotographerInfoRequestDto dto,
      @PathVariable(value = "photographerId") Long photographerId
  ){
    try {
      log.info("photographerId={}, step=작가_정보_저장_시작, status=SUCCESS", photographerId);
      snapService.savePhotographerInfo(dto, photographerId);
      log.info("photographerId={}, step=작가_정보_저장_완료, status=SUCCESS", photographerId);
    } catch (Exception e) {
      log.error("photographerId={}, step=작가_정보_저장_실패, status=FAILED", photographerId, e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("작가 정보를 저장하는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return ResponseEntity.ok().body(new ResultMessageResponseDto("성공적으로 작가 정보가 저장되었습니다."));
  }

  @Operation(summary = "촬영 예시 사진 업로드")
  @PostMapping(value = "photographers/{photographerId}/example-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<JsonListWrapper<GetImageUrlResponseDto>> uploadPhotographerExampleImages(
      @Parameter(description = "이미지 파일들 업로드")
      @RequestParam(value = "images") List<MultipartFile> images,

      @Parameter(description = "사진 작가 id", example = "103")
      @PathVariable("photographerId") Long photographerId
  ){
    List<GetImageUrlResponseDto> dtos;
    try {
      log.info("photographerId={}, step=작가_예시사진_업로드_시작, status=SUCCESS", photographerId);
      dtos = imageService.uploadPhotographerExampleImages(images, photographerId);
      log.info("photographerId={}, step=작가_예시사진_업로드_완료, status=SUCCESS", photographerId);
    } catch (Exception e) {
      log.error("photographerId={}, step=작가_예시사진_업로드_실패, status=FAILED", photographerId, e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("작가 예시 사진 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return ResponseEntity.status(HttpStatus.CREATED).body(new JsonListWrapper<>(dtos));
  }

  @Operation(summary = "촬영 예시 사진 정보 저장 또는 수정")
  @PatchMapping(value = "/photographers/example-images")
  public ResponseEntity<ResultMessageResponseDto> updatePhotographerExampleImagesInfo(
    @Valid @RequestBody UpdatePhotographerExampleImagesInfoRequestDto dto
  ){
    try {
      log.info("step=작가_예시사진_정보_저장_시작, status=SUCCESS");
      snapService.updatePhotographerExampleImagesInfo(dto);
      log.info("step=작가_예시사진_정보_저장_완료, status=SUCCESS");
    } catch (Exception e) {
      log.error("step=작가_예시사진_정보_저장_실패, status=FAILED", e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("작가 예시사진 정보를 저장하는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return ResponseEntity.ok().body(new ResultMessageResponseDto("정상적으로 사진 정보가 저장되었습니다."));
  }

  @Operation(summary = "작가 정보 가져오기")
  @GetMapping("/photographer-infos")
  public ResponseEntity<JsonListWrapper<GetPhotographerInfosRequestDto>> getPhotographerInfos(){
    List<GetPhotographerInfosRequestDto> list;
    try {
      log.info("step=작가_정보_조회_시작, status=SUCCESS");
      list = snapService.getPhotographerInfos();
      log.info("step=작가_정보_조회_완료, status=SUCCESS");
    } catch (Exception e) {
      log.error("step=작가_정보_조회_실패, status=FAILED", e);
      if (e instanceof RingoException re) {
        throw re;
      }
      throw new RingoException("작가 정보를 조회하는데 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return ResponseEntity.ok().body(new JsonListWrapper<>(list));
  }
}
