package com.lingo.lingoproject.image;

import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

  private final ImageService imageService;

  @PostMapping("/profile/upload")
  public ResponseEntity<?> uploadImage(@RequestPart MultipartFile file, @RequestParam("userId") Long userId, @RequestParam("order") int order) throws IOException {
    String imageUrl = imageService.uploadImage(file, userId, order);
    return ResponseEntity.status(HttpStatus.CREATED).body(imageUrl);
  }

  @GetMapping("/profile/imageUrl")
  public ResponseEntity<?> getImageUrl(@RequestParam("userId") Long userId, @RequestParam("order") int order){
    String imageUrl = imageService.getImageUrl(userId, order);
    return ResponseEntity.status(HttpStatus.OK).body(imageUrl);
  }

  @DeleteMapping("/profile")
  public ResponseEntity<?> deleteImage(@RequestParam("userId") Long userId, @RequestParam("order") int order){
    imageService.deleteProfile(userId, order);
    return ResponseEntity.ok().build();
  }
}
