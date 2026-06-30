package com.lingo.lingoproject.community.presentation;

import com.lingo.lingoproject.community.application.CommunityService;
import com.lingo.lingoproject.community.presentation.dto.*;
import com.lingo.lingoproject.matching.application.MatchingPlaceUseCase;
import com.lingo.lingoproject.matching.presentation.dto.GetTypePlaceRequestDto;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController()
@RequiredArgsConstructor
public class CommunityController implements CommunityApi {

  private final CommunityService communityService;
  private final MatchingPlaceUseCase matchingPlaceUseCase;

  @Override
  public ResponseEntity<ApiListResponseDto<GetPostResponseDto>> getPost(String category, Long placeId, int page, int size, User user) {
    log.info("step=게시물_조회_시작, userId={}", user.getId());
    List<GetPostResponseDto> response = communityService.findPosts(user, category, placeId, page, size);
    log.info("step=게시물_조회_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), response));
  }

  @Override
  public ResponseEntity<GetPostResponseDto> getPostById(Long postId, User user) {
    GetPostResponseDto response = communityService.getPostById(postId, user);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<SavePostResponseDto> post(SavePostRequestDto dto, List<MultipartFile> images, User user) {
    log.info("step=게시물_게시_시작, userId={}", user.getId());
    SavePostResponseDto response = communityService.createPost(dto, images, user);
    log.info("step=게시물_게시_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> deletePost(Long postId, User user) {
    log.info("step=게시물_삭제_시작, userId={}", user.getId());
    communityService.deletePost(postId, user);
    log.info("step=게시물_삭제_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "게시물을 성공적으로 삭제했습니다."));
  }

  @Override
  public ResponseEntity<UpdatePostResponseDto> updatePost(Long postId, UpdatePostRequestDto dto, User user) {
    log.info("step=게시물_업데이트_시작, userId={}", user.getId());
    UpdatePostResponseDto response = communityService.updatePost(postId, dto, user);
    log.info("step=게시물_업데이트_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<ApiListResponseDto<GetCommentResponseDto>> getComments(Long postId, User user) {
    log.info("step=댓글_조회_시작, userId={}", user.getId());
    List<GetCommentResponseDto> response = communityService.findCommentsByPost(postId, user);
    log.info("step=댓글_조회_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), response));
  }

  @Override
  public ResponseEntity<CommentResponseDto> comment(CommentRequestDto dto, User user) {
    log.info("step=댓글_업로드_시작, userId={}", user.getId());
    CommentResponseDto response = communityService.createComment(dto, user);
    log.info("step=댓글_업로드_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> updateComment(Long commentId, UpdateCommentRequestDto dto, User user) {
    log.info("step=댓글_업데이트_시작, userId={}", user.getId());
    communityService.updateComment(commentId, dto, user);
    log.info("step=댓글_업데이트_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "댓글 성공적으로 업데이트하였습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> deleteComment(Long commentId, User user) {
    log.info("step=댓글_삭제_시작, userId={}", user.getId());
    communityService.deleteComment(commentId, user);
    log.info("step=댓글_삭제_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "댓글 성공적으로 삭제하였습니다."));
  }

  @Override
  public ResponseEntity<CreateSubCommentResponseDto> createSubComment(CreateSubCommentRequestDto dto, User user) {
    log.info("step=대댓글_생성_시작, userId={}", user.getId());
    Long subCommentId = communityService.createSubComment(dto, user);
    log.info("step=대댓글_생성_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.CREATED).body(new CreateSubCommentResponseDto(subCommentId, ErrorCode.SUCCESS.getCode()));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> updateSubComment(Long subCommentId,
      UpdateSubCommentRequestDto dto, User user) {
    log.info("step=대댓글_업데이트_시작, userId={}", user.getId());
    communityService.updateSubComment(subCommentId, dto, user);
    log.info("step=대댓글_업데이트_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "대댓글을 성공적으로 업데이트했습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> deleteSubComment(Long subCommentId, User user) {
    log.info("step=대댓글_삭제_시작, userId={}", user.getId());
    communityService.deleteSubComment(subCommentId, user);
    log.info("step=대댓글_삭제_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "대댓글을 성공적으로 삭제했습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> likePost(Long postId, User user) {
    log.info("step=게시물_좋아요_시작, userId={}", user.getId());
    communityService.togglePostLike(postId, user);
    log.info("step=게시물_좋아요_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "해당 게시물을 좋아요 혹은 좋아요 취소하였습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> likeComment(Long commentId, User user) {
    log.info("step=댓글_좋아요_시작, userId={}", user.getId());
    communityService.toggleCommentLike(commentId, user);
    log.info("step=댓글_좋아요_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "해당 댓글을 좋아요 혹은 좋아요 취소하였습니다."));
  }

  @Override
  public ResponseEntity<ApiListResponseDto<GetPostResponseDto>> getPlaceRelatedPost(User user, String keyword, String place, int page, int size) {
    log.info("step=게시물_관련_장소/컨텐츠_조회_시작");
    List<GetPostResponseDto> dtos = communityService.searchPostsByKeywordOrPlace(user, keyword, place, page, size);
    log.info("step=게시물_관련_장소/컨텐츠_조회_완료");

    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), dtos));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> savePlace(MultipartFile file){
    communityService.parseExcelAndPersistEntity(file);
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(
        ErrorCode.SUCCESS.getCode(), "성공적으로 업로드 되었습니다."));
  }

  @Override
  public ResponseEntity<GetPlaceResponseDto> getIndividualRecommendationPlaces(User user){
    List<GetPlaceDetailResponseDto> individual = matchingPlaceUseCase.getIndividualUserPlaces(user);
    List<GetTypePlaceRequestDto> common = matchingPlaceUseCase.주제별_장소_컨텐츠_추천(user);

    GetPlaceResponseDto response = new GetPlaceResponseDto(individual, common, ErrorCode.SUCCESS.getCode());

    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<ApiListResponseDto<GetPlaceDetailResponseDto>> getMatchedRecommendationPlaces(User user, Long userId) {
    List<GetPlaceDetailResponseDto> result = matchingPlaceUseCase.매칭된_커플을_위한_장소_컨텐츠_추천(user, userId);
    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result));
  }

  @Override
  public ResponseEntity<ApiListResponseDto<GetPlaceDetailResponseDto>> getRankedPagedPlaces(User user, int page, int size) {
    List<GetPlaceDetailResponseDto> rankedPlaces = matchingPlaceUseCase.getRankedPagedPlaces(user, page, size);
    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), rankedPlaces));
  }

  @Override
  public ResponseEntity<GetPlaceDetailResponseDto> getDetailPlaceInfo(User user, Long placeId) {
    GetPlaceDetailResponseDto response = communityService.getDetailPlaceInfo(user, placeId);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<ApiListResponseDto<GetPlaceDetailResponseDto>> getPlacesByType(User user, String type, int page, int size) {
      List<GetPlaceDetailResponseDto> result = matchingPlaceUseCase.타입_기반_장소_컨텐츠_추천(type, user);
      return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), result));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> updatePlaceClickCount(Long placeId) {
    communityService.updatePlaceClickCount(placeId);
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "성공적으로 업데이트 하였습니다."));
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> scrapPlace(ScrapPlaceRequestDto request, User user) {
    communityService.scrapPlace(request.placeId(), user);
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "장소를 스크랩했습니다."));
  }

  @Override
  public ResponseEntity<ApiListResponseDto<GetPlaceDetailResponseDto>> getScrappedPlace(User user) {
    List<GetPlaceDetailResponseDto> scrappedPlaces = communityService.getScrappedPlace(user);
    return ResponseEntity.status(HttpStatus.OK).body(new ApiListResponseDto<>(ErrorCode.SUCCESS.getCode(), scrappedPlaces));
  }

  @Override
  public ResponseEntity<ApiListResponseDto<SearchPlaceRequestDto>> searchPlacesByKeywords(String keyword) {
    List<SearchPlaceRequestDto> response = communityService.searchPlaceByKeyword(keyword);
    return ResponseEntity.status(HttpStatus.OK).body(
        new ApiListResponseDto<>(
            ErrorCode.SUCCESS.getCode(),
            response
        ));
  }

  @Override
  public ResponseEntity<PlaceSummaryResponseDto> getPlaceSummary(Long placeId) {
    PlaceSummaryResponseDto response = communityService.getPlaceSummary(placeId);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<ResultMessageResponseDto> updatePlace(Long placeId, UpdatePlaceRequestDto dto) {
    communityService.updatePlace(dto);
    return ResponseEntity.status(HttpStatus.OK).body(new ResultMessageResponseDto(ErrorCode.SUCCESS.getCode(), "성공적으로 업데이트 되었습니다."));
  }

  @Override
  public ResponseEntity<Map<String, InputStatusResponseDto>> getPlaceInputStatus() {
    Map<String, InputStatusResponseDto> response = communityService.getInputStatus();
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @PostMapping("/api/place")
  public ResponseEntity<?> saveParsedExcelData(@RequestBody List<SaveParsedPlaceRequest> dto){
    communityService.updatePlaceDetail(dto);
    return ResponseEntity.status(HttpStatus.OK).body("yes");
  }
}
