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

  private static final int 최대_피드사진_업로드_개수 = 9;


  public ProfileTransactionService(ProfileRepository profileRepository,
      UserQueryUseCase userQueryUseCase, FeedImageRepository feedImageRepository) {
    this.profileRepository = profileRepository;
    this.userQueryUseCase = userQueryUseCase;
    this.feedImageRepository = feedImageRepository;
  }

  @Transactional
  public GetImageUrlResponseDto 프로필_url_저장과_프로필_제출로_상태변경(String imageUrl, User user){
    Profile savedProfile = 프로필_url_저장(user, imageUrl);
    프로필_제출로_상태_변경(user);

    log.info("userId={}, profileUrl={}, status={}", user.getId(), savedProfile.getImageUrl(), user.getStatus());

    return new GetImageUrlResponseDto(
        ErrorCode.SUCCESS.getCode(), savedProfile.getImageUrl(), savedProfile.getId());
  }

  private Profile 프로필_url_저장(User user, String imageUrl) {
    Profile profile = Profile.프로필_객체_생성(user, imageUrl);
    Profile saved = profileRepository.save(profile);
    user.setProfile(saved);
    userQueryUseCase.save(user);
    return saved;
  }

  public void 프로필_제출로_상태_변경(User user) {
    user.setStatus(SignupStatus.SUBMITTED);
    userQueryUseCase.save(user);
  }

  @Transactional
  public void 프로필_이미지_업데이트(Profile profile, String newImageUrl){
    profile.setImageUrl(newImageUrl);
    profileRepository.save(profile);
  }

  @Transactional
  public void 프로필_이미지_삭제(Profile profile){
    profileRepository.delete(profile);
  }

  @Transactional
  public List<GetImageUrlResponseDto> 피드_이미지_업로드(User user, List<String> feedImageUrl, List<FeedImageDataRequestDto> requests){

    int 이미_존재하는_피드사진_개수 = feedImageRepository.countByUserWithLock(user);
    if (이미_존재하는_피드사진_개수 + requests.size() > 최대_피드사진_업로드_개수) {
      throw new RingoException("최대 업로드 개수를 초과하였습니다.", ErrorCode.OVERFLOW);
    }

    List<FeedImage> feedImages = IntStream.range(0, feedImageUrl.size())
        .mapToObj(i -> FeedImage.of(user, feedImageUrl.get(i), requests.get(i).getContent()))
        .toList();

    return feedImageRepository.saveAll(feedImages)
        .stream()
        .map(img -> new GetImageUrlResponseDto(
            ErrorCode.SUCCESS.getCode(), img.getImageUrl(), img.getId())
        )
        .toList();
  }

  @Transactional
  public FeedImage 피드_이미지_업데이트(FeedImage 피드_이미지){
    return feedImageRepository.save(피드_이미지);
  }
}
