package com.lingo.lingoproject.admin.application;

import com.lingo.lingoproject.admin.presentation.dto.PostProfileReviewRequestDto;
import com.lingo.lingoproject.admin.presentation.dto.ProfileReviewListResponseDto;
import com.lingo.lingoproject.admin.presentation.dto.ProfileReviewResponseDto;
import com.lingo.lingoproject.image.application.S3ImageStorageService;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.ProfileRepository;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileInspectUseCase {
  private final UserQueryUseCase userQueryUseCase;
  private final ProfileRepository profileRepository;
  private final S3ImageStorageService s3ImageStorageService;

  public ProfileReviewListResponseDto getProfileReviews(String status, int page, int size){
    Pageable pageable = PageRequest.of(page, size);
    SignupStatus value = switch (status){
      case "PENDING" -> SignupStatus.SUBMITTED;
      case "REJECTED" -> SignupStatus.REJECTED;
      case "APPROVED" -> SignupStatus.COMPLETED;
      default -> throw new RingoException("status의 parameter값이 적절하지 않습니다.", ErrorCode.BAD_PARAMETER);
    };
    Page<User> list = userQueryUseCase.findAllByStatus(value, pageable);
    long count = userQueryUseCase.countByStatus(value);
    List<ProfileReviewResponseDto> result = list.stream()
        .map(this::buildProfileReviewResponseDto)
        .toList();
    ProfileReviewListResponseDto response = ProfileReviewListResponseDto.builder()
        .content(result)
        .page(page)
        .size(size)
        .totalElements(count)
        .totalPages(size != 0 ? (int) Math.ceil(count / (float) size) : 0)
        .build();
    return response;
  }

  public ProfileReviewResponseDto buildProfileReviewResponseDto(User user){
    Profile profile = user.getProfile();
    String imageUrl = resolveReviewImageUrl(profile, user.getStatus());
    List<String> imageUrls = List.of(imageUrl);
    String status = switch (user.getStatus()){
      case SignupStatus.COMPLETED -> "APPROVED";
      case SignupStatus.SUBMITTED -> "PENDING";
      case SignupStatus.REJECTED -> "REJECTED";
      default -> "PENDING";
    };
    ProfileReviewResponseDto result = ProfileReviewResponseDto.builder()
        .id(user.getId().toString())
        .userId(user.getId().toString())
        .nickname(user.getNickname())
        .imageUrls(imageUrls)
        .submittedAt(profile != null ? profile.getCreatedAt().toString() : null)
        .status(status)
        .build();
    return result;
  }

  private String resolveReviewImageUrl(Profile profile, SignupStatus status) {
    if (profile == null) return null;
    return switch (status) {
      case SignupStatus.COMPLETED -> profile.getImageUrl();
      case SignupStatus.REJECTED, SignupStatus.SUBMITTED -> profile.getInspectProfileUrl();
      default -> profile.getInspectProfileUrl();
    };
  }

  @Transactional
  public void postProfileReview(Long id, PostProfileReviewRequestDto request){
    User user = userQueryUseCase.유저_찾기_혹은_오류(id);
    Profile profile = user.getProfile();
    if (profile == null) throw new RingoException("프로필을 찾을 수 없습니다.", ErrorCode.BAD_REQUEST);
    SignupStatus status = switch (request.decision()){
      case "APPROVE" -> SignupStatus.COMPLETED;
      case "REJECT" -> SignupStatus.REJECTED;
      default -> throw new RingoException("status 값이 올바르지 않습니다.", ErrorCode.BAD_PARAMETER);
    };

    if (status == SignupStatus.COMPLETED) {
      s3ImageStorageService.프로필_검수_승인_처리(profile);
    }

    user.setStatus(status);
    userQueryUseCase.save(user);

    if (request.reason() != null) {
      profile.setReason(request.reason());
      profileRepository.save(profile);
    }
  }
}
