package com.lingo.lingoproject.snap;

import com.lingo.lingoproject.domain.PhotographerImage;
import com.lingo.lingoproject.domain.PhotographerInfo;
import com.lingo.lingoproject.domain.SnapApply;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.PhotographerImageRepository;
import com.lingo.lingoproject.repository.PhotographerInfoRepository;
import com.lingo.lingoproject.repository.SnapApplyRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.repository.impl.PhotographerInfoRepositoryImpl;
import com.lingo.lingoproject.snap.dto.ApplySnapShootingRequestDto;
import com.lingo.lingoproject.snap.dto.GetPhotographerInfosResponseDto;
import com.lingo.lingoproject.snap.dto.UpdatePhotographerExampleImagesInfoRequestDto;
import com.lingo.lingoproject.snap.dto.SavePhotographerInfoRequestDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SnapService {

  private final UserRepository userRepository;
  private final SnapApplyRepository snapApplyRepository;
  private final PhotographerInfoRepository photographerInfoRepository;
  private final PhotographerImageRepository photographerImageRepository;
  private final PhotographerInfoRepositoryImpl photographerInfoRepositoryImpl;

  public void applySnapShooting(ApplySnapShootingRequestDto dto){

    User photographer = userRepository.findById(dto.photographerId())
        .orElseThrow(() -> new RingoException("해당 촬영 기사가 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    SnapApply snapApply = SnapApply.builder()
        .photographer(photographer)
        .userId(dto.userId())
        .duration(dto.snapDuration())
        .snapStartedAt(LocalDateTime.parse(dto.snapStartedAt(), formatter))
        .snapLocation(dto.snapLocation())
        .meetingLocation(dto.meetingLocation())
        .build();
    snapApplyRepository.save(snapApply);
  }

  public void savePhotographerInfo(SavePhotographerInfoRequestDto dto, Long photographerId){

    User photographer = userRepository.findById(photographerId)
        .orElseThrow(() -> new RingoException("해당 촬영 기사가 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    PhotographerInfo photographerInfo = PhotographerInfo.builder()
        .photographer(photographer)
        .content(dto.content())
        .instagramId(dto.instagramId())
        .chatIntro(dto.chatIntro())
        .build();

    photographerInfoRepository.save(photographerInfo);
  }

  public void updatePhotographerExampleImagesInfo(UpdatePhotographerExampleImagesInfoRequestDto dto){
    PhotographerImage image = photographerImageRepository.findById(dto.imageId())
        .orElseThrow(() -> new RingoException("해당 사진을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    image.setSnapLocation(dto.snapLocation());

    try {
      if (dto.snapDate() != null) {
        image.setSnapDate(LocalDate.parse(dto.snapDate()));
      } else {
        image.setSnapDate(null);
      }
    }catch (Exception e){
      log.error("스냅 촬영일 파싱 실패. imageId: {}, snapDate: {}", dto.imageId(), dto.snapDate(), e);
      throw new RingoException("시간을 파싱하던 중 오류가 발생하였습니다.", ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    photographerImageRepository.save(image);
  }

  public List<GetPhotographerInfosResponseDto> getPhotographerInfos(){
    return photographerInfoRepositoryImpl.getPhotographerInfos();
  }
}
