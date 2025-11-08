package com.lingo.lingoproject.snap;

import com.lingo.lingoproject.domain.PhotographerImage;
import com.lingo.lingoproject.domain.PhotographerInfo;
import com.lingo.lingoproject.domain.SnapApply;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.repository.PhotographerImageRepository;
import com.lingo.lingoproject.repository.PhotographerInfoRepository;
import com.lingo.lingoproject.repository.SnapApplyRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.repository.impl.PhotographerInfoRepositoryImpl;
import com.lingo.lingoproject.snap.dto.ApplySnapShootingRequestDto;
import com.lingo.lingoproject.snap.dto.GetPhotographerInfosRequestDto;
import com.lingo.lingoproject.snap.dto.UpdatePhotographerExampleImagesInfoRequestDto;
import com.lingo.lingoproject.snap.dto.SavePhotographerInfoRequestDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SnapService {

  private final UserRepository userRepository;
  private final SnapApplyRepository snapApplyRepository;
  private final PhotographerInfoRepository photographerInfoRepository;
  private final PhotographerImageRepository photographerImageRepository;
  private final PhotographerInfoRepositoryImpl photographerInfoRepositoryImpl;

  public void applySnapShooting(ApplySnapShootingRequestDto dto){

    User photographer = userRepository.findById(dto.photographerId())
        .orElseThrow(() -> new RingoException("해당 촬영 기사가 없습니다.", HttpStatus.BAD_REQUEST));

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

  public void savePhotographerInfo(SavePhotographerInfoRequestDto dto){

    User photographer = userRepository.findById(dto.photographerId())
        .orElseThrow(() -> new RingoException("해당 촬영 기사가 없습니다.", HttpStatus.BAD_REQUEST));

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
        .orElseThrow(() -> new RingoException("해당 사진을 찾을 수 없습니다.", HttpStatus.BAD_REQUEST));
    image.setSnapLocation(dto.snapLocation());
    if(dto.snapDate() != null) {
      image.setSnapDate(LocalDate.parse(dto.snapDate()));
    }else{
      image.setSnapDate(null);
    }

    photographerImageRepository.save(image);
  }

  public List<GetPhotographerInfosRequestDto> getPhotographerInfos(){
    return photographerInfoRepositoryImpl.getPhotographerInfos();
  }
}
