package com.lingo.lingoproject.snap.application;

import com.lingo.lingoproject.shared.domain.model.PhotographerImage;
import com.lingo.lingoproject.shared.domain.model.PhotographerInfo;
import com.lingo.lingoproject.shared.domain.model.SnapApply;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.persistence.PhotographerImageRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PhotographerInfoRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.SnapApplyRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.snap.presentation.dto.ApplySnapShootingRequestDto;
import com.lingo.lingoproject.snap.presentation.dto.GetPhotographerInfosResponseDto;
import com.lingo.lingoproject.snap.presentation.dto.UpdatePhotographerExampleImagesInfoRequestDto;
import com.lingo.lingoproject.snap.presentation.dto.SavePhotographerInfoRequestDto;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 스냅 촬영 신청 및 작가 정보 관리 비즈니스 로직을 담당하는 서비스.
 *
 * <h2>주요 기능</h2>
 * <ul>
 *   <li>스냅 촬영 신청 — 사용자가 특정 작가에게 촬영을 예약</li>
 *   <li>작가 정보 등록 — 자기 소개, 인스타그램 ID, 채팅 인사말 저장</li>
 *   <li>예시 이미지 정보 수정 — 촬영 장소·날짜 변경</li>
 *   <li>작가 정보 목록 조회</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SnapService {

  private final UserRepository userRepository;
  private final SnapApplyRepository snapApplyRepository;
  private final PhotographerInfoRepository photographerInfoRepository;
  private final PhotographerImageRepository photographerImageRepository;

  /**
   * 스냅 촬영을 신청한다.
   *
   * <p>신청자가 원하는 작가 ID, 촬영 시작 시각, 촬영 시간, 촬영 장소, 만남 장소를
   * {@link SnapApply} 엔티티로 저장합니다.</p>
   *
   * @param dto 스냅 촬영 신청 정보 (작가 ID, 신청자 ID, 촬영 시작 시각, 촬영 시간, 촬영·만남 장소)
   * @throws RingoException 해당 작가가 존재하지 않는 경우
   */
  public void applySnapShooting(ApplySnapShootingRequestDto dto){

    User photographer = userRepository.findById(dto.photographerId())
        .orElseThrow(() -> new RingoException("해당 촬영 기사가 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    snapApplyRepository.save(SnapApply.of(photographer, dto.userId(),
        LocalDateTime.parse(dto.snapStartedAt(), formatter), dto.snapDuration(),
        dto.snapLocation(), dto.meetingLocation()));
  }

  /**
   * 작가 정보를 등록한다.
   *
   * <p>자기 소개({@code content}), 인스타그램 ID, 채팅 인사말을 포함한
   * {@link PhotographerInfo} 엔티티를 저장합니다.</p>
   *
   * @param dto            작가 정보 DTO (내용, 인스타그램 ID, 채팅 인사말)
   * @param photographerId 작가로 등록할 유저 ID
   * @throws RingoException 해당 유저가 존재하지 않는 경우
   */
  public void savePhotographerInfo(SavePhotographerInfoRequestDto dto, Long photographerId){

    User photographer = userRepository.findById(photographerId)
        .orElseThrow(() -> new RingoException("해당 촬영 기사가 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    photographerInfoRepository.save(PhotographerInfo.of(photographer, dto.content(), dto.instagramId(), dto.chatIntro()));
  }

  /**
   * 작가 예시 이미지의 촬영 장소·날짜를 수정한다.
   *
   * <p>날짜 문자열이 null이면 날짜를 null로 설정하고,
   * 파싱에 실패하면 {@code 500 INTERNAL_SERVER_ERROR}를 반환합니다.</p>
   *
   * @param dto 수정할 이미지 ID, 촬영 장소, 촬영 날짜(nullable, "yyyy-MM-dd" 형식)
   * @throws RingoException 이미지가 존재하지 않거나 날짜 파싱에 실패한 경우
   */
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

  /**
   * 등록된 모든 작가 정보 목록을 조회한다.
   *
   * @return 작가 정보 목록 (이미지, 자기 소개, 인스타그램 ID 포함)
   */
  public List<GetPhotographerInfosResponseDto> getPhotographerInfos(){
    return photographerInfoRepository.getPhotographerInfos();
  }
}
