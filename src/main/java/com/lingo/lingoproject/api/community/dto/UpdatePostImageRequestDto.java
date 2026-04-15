package com.lingo.lingoproject.api.community.dto;

import jakarta.validation.constraints.NotBlank;
import org.springframework.web.multipart.MultipartFile;

public record UpdatePostImageRequestDto(
    @NotBlank Long imageId,
    MultipartFile imageFile
) {

}
