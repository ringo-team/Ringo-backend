package com.lingo.lingoproject.image.application;

import com.lingo.lingoproject.image.dto.FeedImageDataRequestDto;
import com.lingo.lingoproject.image.dto.GetImageUrlResponseDto;
import com.lingo.lingoproject.shared.domain.model.FeedImage;
import com.lingo.lingoproject.shared.domain.model.Profile;
import com.lingo.lingoproject.shared.domain.model.SignupStatus;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.FeedImageRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.ProfileRepository;
import com.lingo.lingoproject.user.application.UserQueryUseCase;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProfileTransactionService {

  private final ProfileRepository profileRepository;
  private final UserQueryUseCase userQueryUseCase;
  private final FeedImageRepository feedImageRepository;

  private static final int MAX_FEED_IMAGE_COUNT = 9;


  public ProfileTransactionService(ProfileRepository profileRepository,
      UserQueryUseCase userQueryUseCase, FeedImageRepository feedImageRepository) {
    this.profileRepository = profileRepository;
    this.userQueryUseCase = userQueryUseCase;
    this.feedImageRepository = feedImageRepository;
  }

  @Transactional
  public GetImageUrlResponseDto saveProfileAndComplete(String imageUrl, User user){
    Profile savedProfile = saveProfile(user, imageUrl);
    completeSignupStatus(user);

    log.info("userId={}, profileUrl={}, status={}", user.getId(), savedProfile.getImageUrl(), user.getStatus());

    return new GetImageUrlResponseDto(
        ErrorCode.SUCCESS.getCode(), savedProfile.getImageUrl(), savedProfile.getId());
  }

  private Profile saveProfile(User user, String imageUrl) {
    Profile profile = Profile.of(user, imageUrl);
    Profile saved = profileRepository.save(profile);
    user.setProfile(saved);
    userQueryUseCase.save(user);
    return saved;
  }

  public void completeSignupStatus(User user) {
    user.setStatus(SignupStatus.COMPLETED);
    userQueryUseCase.save(user);
  }

  @Transactional
  public void updateProfileImageUrl(Profile profile, String newImageUrl){
    profile.setImageUrl(newImageUrl);
    profileRepository.save(profile);
  }

  @Transactional
  public void deleteProfile(Profile profile){
    profileRepository.delete(profile);
  }

  @Transactional
  public List<GetImageUrlResponseDto> uploadFeedImages(User user, List<String> feedImageUrl, List<FeedImageDataRequestDto> requests){

    int existingCount = feedImageRepository.countByUserWithLock(user);
    if (existingCount + requests.size() > MAX_FEED_IMAGE_COUNT) {
      throw new RingoException("최대 업로드 개수를 초과하였습니다.", ErrorCode.OVERFLOW);
    }

    List<FeedImage> feedImages = IntStream.range(0, feedImageUrl.size())
        .mapToObj(i -> FeedImage.of(user, feedImageUrl.get(i), requests.get(i).getContent()))
        .toList();

    return feedImageRepository.saveAll(feedImages)
        .stream()
        .map(img -> new GetImageUrlResponseDto(
            ErrorCode.SUCCESS.getCode(), img.getImageUrl(), img.getId()))
        .toList();
  }
}
