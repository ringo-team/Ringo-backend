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

  public Profile 유저_프로필_조회_없으면_NULL반환(User user) {
    return profileRepository.findByUser(user).orElse(null);
  }


  @Transactional
  public void 프로필_제출로_상태_변경(User user) {
    user.setStatus(SignupStatus.SUBMITTED);
    userQueryUseCase.save(user);
  }

  @Transactional
  public Profile 프로필_이미지_업데이트(User user, Profile profile, String newImageUrl){
    profile.setInspectProfileUrl(newImageUrl);
    Profile saved = profileRepository.save(profile);
    user.setProfile(saved);
    userQueryUseCase.save(user);
    return saved;
  }

  @Transactional
  public void 프로필_검수_승인(Profile profile) {
    profile.setImageUrl(profile.getInspectProfileUrl());
    profile.setInspectProfileUrl(null);
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

    return buildFeedImageUploadResponseDto(feedImageRepository.saveAll(feedImages));
  }

  public List<GetImageUrlResponseDto> buildFeedImageUploadResponseDto(List<FeedImage> feedImages){
    return feedImages
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

  @Transactional
  public FeedImage 피드_이미지_조회(Long 피드_이미지_id){
    return feedImageRepository.findById(피드_이미지_id)
            .orElseThrow(() -> new RingoException("피드 사진을 찾을 수 없습니다.", ErrorCode.NOT_FOUND));
  }

  @Transactional
  public void 피드_이미지_삭제(FeedImage feedImage){
    feedImageRepository.delete(feedImage);
  }

  public List<FeedImage> 해당_유저의_모든_피드_이미지_조회(User user){
    return feedImageRepository.findAllByUser(user);
  }

  @Transactional
  public void 해당_유저의_모든_피드_이미지_삭제(User user){
    feedImageRepository.findAllByUser(user);
  }
}
