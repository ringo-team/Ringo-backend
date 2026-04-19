package com.lingo.lingoproject.community.presentation;
import com.lingo.lingoproject.community.application.CommunityService;
import com.lingo.lingoproject.community.presentation.dto.CommentRequestDto;
import com.lingo.lingoproject.community.presentation.dto.CommentResponseDto;
import com.lingo.lingoproject.community.presentation.dto.CreateSubCommentRequestDto;
import com.lingo.lingoproject.community.presentation.dto.CreateSubCommentResponseDto;
import com.lingo.lingoproject.community.presentation.dto.GetCommentResponseDto;
import com.lingo.lingoproject.community.presentation.dto.GetPostResponseDto;
import com.lingo.lingoproject.community.presentation.dto.SavePostRequestDto;
import com.lingo.lingoproject.community.presentation.dto.SavePostResponseDto;
import com.lingo.lingoproject.community.presentation.dto.UpdateCommentRequestDto;
import com.lingo.lingoproject.community.presentation.dto.UpdatePostRequestDto;
import com.lingo.lingoproject.community.presentation.dto.UpdatePostResponseDto;
import com.lingo.lingoproject.community.presentation.dto.UpdateSubCommentRequestDto;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController()
@RequiredArgsConstructor
public class CommunityController implements CommunityApi {

  private final CommunityService communityService;

  @Override
  public ResponseEntity<List<GetPostResponseDto>> getPost(String topic, int page, int size, User user) {
    log.info("step=게시물_조회_시작, userId={}", user.getId());
    List<GetPostResponseDto> response = communityService.findPosts(topic, page, size);
    log.info("step=게시물_조회_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @Override
  public ResponseEntity<SavePostResponseDto> post(SavePostRequestDto dto, List<MultipartFile> images, User user) {
    log.info("step=게시물_게시_시작, userId={}", user.getId());
    SavePostResponseDto response = communityService.createPost(dto, images);
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
  public ResponseEntity<List<GetCommentResponseDto>> getComments(Long postId, User user) {
    log.info("step=댓글_조회_시작, userId={}", user.getId());
    List<GetCommentResponseDto> response = communityService.findCommentsByPost(postId);
    log.info("step=댓글_조회_완료, userId={}", user.getId());
    return ResponseEntity.status(HttpStatus.OK).body(response);
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
}
