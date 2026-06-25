package com.lingo.lingoproject.admin.application;

import com.lingo.lingoproject.admin.presentation.dto.ProfileReviewResponseDto;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import jakarta.xml.bind.annotation.XmlType.DEFAULT;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileInspectUseCase {
  private final UserRepository userRepository;
  public List<ProfileReviewResponseDto> getProfileReviews(String status, int page, int size){
    Pageable pageable = PageRequest.of(page, size);
    SignupStatus value = switch (status){
      case "PENDING" -> SignupStatus.SUBMITTED;
      case "REJECTED" -> SignupStatus.REJECTED;
      case "APPROVED" -> SignupStatus.COMPLETED;
      default -> throw new RingoException("status의 parameter값이 적절하지 않습니다.", ErrorCode.BAD_PARAMETER);
    };
    Page<User> list = userRepository.findAllByStatus(value, pageable);
    return list.stream()
        .map(this::buildProfileReviewResponseDto)
        .toList();
  }
  public ProfileReviewResponseDto buildProfileReviewResponseDto(User user){
    Profile profile = user.getProfile();
    List<String> imageUrls = List.of(profile != null ? profile.getImageUrl() : null);
    String status = switch (user.getStatus()){
      case SignupStatus.COMPLETED -> "APPROVED";
      case SignupStatus.SUBMITTED -> "PENDING";
      case SignupStatus.REJECTED -> "REJECTED";
      default -> "PENDING";
    };
    return ProfileReviewResponseDto.builder()
        .id("user_" + user.getId().toString())
        .userId(user.getId().toString())
        .nickname(user.getNickname())
        .imageUrls(imageUrls)
        .submittedAt(profile != null ? profile.getCreatedAt().toString() : null)
        .status(status)
        .build();
  }
}
