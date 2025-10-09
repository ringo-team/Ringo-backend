package com.lingo.lingoproject.image;

import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/profiles")
@RequiredArgsConstructor
public class ImageController {

  private final ImageService imageService;

  /**
   *  order가 0 이면 안됨
   * @param file
   * @param userId
   * @param order
   * @return
   * @throws IOException
   */
  @PostMapping("/{userId}")
  public ResponseEntity<?> uploadImage(@RequestPart MultipartFile file, @PathVariable("userId") Long userId, @RequestParam("order") int order) throws IOException {
    if(order == 0){
      throw new IllegalArgumentException("order 값 0는 전체조회 용도 입니다.");
    }
    String imageUrl = imageService.uploadImage(file, userId, order);
    return ResponseEntity.status(HttpStatus.CREATED).body(imageUrl);
  }

  /**
   *  order = 0 일 때는 전체 조회
   * @param userId
   * @param order
   * @return
   */
  @GetMapping("/{userId}")
  public ResponseEntity<?> getImageUrl(@PathVariable("userId") Long userId, @RequestParam("order") int order){
    if(order == 0){
      List<GetImageUrlRequestDto> list = imageService.getAllImageUrls(userId);
      return ResponseEntity.status(HttpStatus.OK).body(new GetAllImageUrlsRequestDto(list));
    }
    GetImageUrlRequestDto dto = imageService.getImageUrl(userId, order);
    return ResponseEntity.status(HttpStatus.OK).body(dto);
  }

  @DeleteMapping("/{userId}")
  public ResponseEntity<?> deleteImage(@PathVariable("userId") Long userId, @RequestParam("order") int order){
    imageService.deleteProfile(userId, order);
    return ResponseEntity.ok().build();
  }
}
