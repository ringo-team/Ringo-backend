package com.lingo.lingoproject.community.application;

import com.lingo.lingoproject.community.domain.event.CommentCreatedEvent;
import com.lingo.lingoproject.community.domain.event.PostCreatedEvent;
import com.lingo.lingoproject.community.domain.service.CommunityDomainService;
import com.lingo.lingoproject.community.presentation.dto.CommentRequestDto;
import com.lingo.lingoproject.community.presentation.dto.CommentResponseDto;
import com.lingo.lingoproject.community.presentation.dto.CreateSubCommentRequestDto;
import com.lingo.lingoproject.community.presentation.dto.GetCommentResponseDto;
import com.lingo.lingoproject.community.presentation.dto.GetPlaceDetailResponseDto;
import com.lingo.lingoproject.community.presentation.dto.GetPostImageResponseDto;
import com.lingo.lingoproject.community.presentation.dto.GetPostResponseDto;
import com.lingo.lingoproject.community.presentation.dto.SavePostImageResponseDto;
import com.lingo.lingoproject.community.presentation.dto.SavePostRequestDto;
import com.lingo.lingoproject.community.presentation.dto.SavePostResponseDto;
import com.lingo.lingoproject.community.presentation.dto.UpdateCommentRequestDto;
import com.lingo.lingoproject.community.presentation.dto.UpdatePostImageRequestDto;
import com.lingo.lingoproject.community.presentation.dto.UpdatePostImageResponseDto;
import com.lingo.lingoproject.community.presentation.dto.UpdatePostRequestDto;
import com.lingo.lingoproject.community.presentation.dto.UpdatePostResponseDto;
import com.lingo.lingoproject.community.presentation.dto.UpdateSubCommentRequestDto;
import com.lingo.lingoproject.shared.domain.elastic.PostDocument;
import com.lingo.lingoproject.shared.domain.model.Comment;
import com.lingo.lingoproject.shared.domain.model.CommentLikeUserMapping;
import com.lingo.lingoproject.shared.domain.model.Place;
import com.lingo.lingoproject.shared.domain.model.Post;
import com.lingo.lingoproject.shared.domain.model.PostCategory;
import com.lingo.lingoproject.shared.domain.model.PostImage;
import com.lingo.lingoproject.shared.domain.model.PostLikeUserMapping;
import com.lingo.lingoproject.shared.domain.event.DomainEventPublisher;
import com.lingo.lingoproject.shared.domain.model.Survey;
import com.lingo.lingoproject.shared.domain.model.SurveyCategory;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.domain.model.UserScrapPlace;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.exception.RingoException;
import com.lingo.lingoproject.shared.infrastructure.elastic.PostSearchRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.CommentLikeUserMappingRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.CommentRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PlaceRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PostImageRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PostLikeUserMappingRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PostRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.SubCommentRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.UserScrapPlaceRepository;
import com.lingo.lingoproject.shared.infrastructure.storage.S3ImageStorageService;
import com.lingo.lingoproject.shared.utils.GenericUtils;
import jakarta.transaction.Transactional;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 커뮤니티 게시물·댓글·좋아요 관련 비즈니스 로직을 담당하는 서비스.
 *
 * <h2>주요 기능</h2>
 * <ul>
 *   <li>게시물 CRUD — S3 이미지 업로드·삭제와 함께 처리</li>
 *   <li>댓글 / 대댓글 CRUD — 작성자 본인만 수정·삭제 가능</li>
 *   <li>게시물 좋아요 / 댓글 좋아요 토글 — 매핑 테이블({@link PostLikeUserMapping}, {@link CommentLikeUserMapping})로 관리</li>
 *   <li>게시물 목록 페이지네이션 — 추천 장소 + 토픽 기준 필터링 지원</li>
 * </ul>
 *
 * <h2>이미지 처리 흐름</h2>
 * <p>게시물 생성 시 {@link S3ImageStorageService}를 통해 각 이미지의 부적절한 콘텐츠를 검사한 후
 * S3에 업로드하고, {@link PostImage} 엔티티로 저장합니다.
 * 게시물 삭제 시에는 연결된 S3 오브젝트를 먼저 제거한 뒤 DB에서 삭제합니다.</p>
 *
 * <h2>소유권 검증</h2>
 * <p>게시물·댓글·대댓글의 수정·삭제는 {@link #} 등의 헬퍼를 통해
 * 작성자 본인 여부를 검증하며, 권한이 없으면 {@code 403 FORBIDDEN}을 반환합니다.</p>
 *
 * <h2>좋아요 토글 규칙</h2>
 * <p>이미 좋아요를 누른 경우 취소(매핑 삭제 + 카운트 감소)하고,
 * 아직 누르지 않은 경우 추가(매핑 저장 + 카운트 증가)합니다.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final CommentRepository commentRepository;
  private final S3ImageStorageService imageService;
  private final PostImageRepository postImageRepository;
  private final PostLikeUserMappingRepository postLikeUserMappingRepository;
  private final CommentLikeUserMappingRepository commentLikeUserMappingRepository;
  private final SubCommentRepository subCommentRepository;
  private final CommunityDomainService communityDomainService;
  private final DomainEventPublisher eventPublisher;
  private final PostSearchRepository postSearchRepository;
  private final PlaceRepository placeRepository;
  private final UserScrapPlaceRepository userScrapPlaceRepository;

  /**
   * 게시물을 생성한다.
   *
   * <p>이미지가 있는 경우 부적절한 콘텐츠 검사 후 S3에 업로드하고 {@link PostImage}로 저장합니다.
   * 부적절한 콘텐츠로 판정된 이미지는 업로드를 건너뜁니다.</p>
   *
   * @param dto    게시물 작성 요청 DTO (작성자 ID, 추천 장소 ID, 제목, 내용, 토픽 포함)
   * @param images 첨부 이미지 목록 (빈 리스트 허용)
   * @return 저장된 게시물 ID, 업로드된 이미지 목록, 처리 결과 코드
   * @throws RingoException 작성자 또는 추천 장소가 존재하지 않는 경우
   */
  @Transactional
  public SavePostResponseDto createPost(SavePostRequestDto dto, List<MultipartFile> images) {
    log.info("step=게시물_작성_시작, userId={}, topic={}", dto.userId(), dto.category());

    User author = findUserOrThrow(dto.userId());
    PostCategory postCategory = GenericUtils.validateAndReturnEnumValue(PostCategory.values(), dto.category());

    Post savedPost = postRepository.save(Post.of(author, dto.title(), dto.content(), postCategory));

    log.info("step=게시물_저장_완료, postId={}, userId={}, imageCount={}", savedPost.getId(), dto.userId(), images == null ? 0 : images.size());

    eventPublisher.publish(new PostCreatedEvent(savedPost));

    List<SavePostImageResponseDto> savedPostImages = uploadAndSavePostImages(images, savedPost);
    return new SavePostResponseDto(savedPost.getId(), savedPostImages, ErrorCode.SUCCESS.getCode());
  }

  /**
   * 게시물을 수정한다.
   *
   * <p>게시물 소유자 여부를 검증한 뒤, 제목·내용·토픽·이미지를 선택적으로 변경합니다.
   * 이미지 교체 시 기존 S3 오브젝트를 삭제하고 새 이미지를 업로드합니다.</p>
   *
   * @param postId 수정할 게시물 ID
   * @param dto    수정 요청 DTO (변경할 필드만 포함, null이면 변경하지 않음)
   * @param user   요청 사용자 (소유권 검증에 사용)
   * @return 업데이트된 이미지 목록 + 처리 결과 코드
   * @throws RingoException 게시물이 존재하지 않거나 소유자가 아닌 경우
   */
  @Transactional
  public UpdatePostResponseDto updatePost(Long postId, UpdatePostRequestDto dto, User user) {
    Post post = findPostOrThrow(postId);
    communityDomainService.validatePostOwnership(post, user);
    post.updatePost(dto);

    List<UpdatePostImageResponseDto> updatedImages = updatePostImageAndGetResponseDto(dto.imagelist());

    postRepository.save(post);
    log.info("step=게시물_수정_완료, postId={}, userId={}", postId, user.getId());

    return new UpdatePostResponseDto(updatedImages, ErrorCode.SUCCESS.getCode());
  }

  private List<UpdatePostImageResponseDto> updatePostImageAndGetResponseDto(List<UpdatePostImageRequestDto> imagelist){
    return imagelist.stream()
        .map(this::replacePostImage)
        .toList();
  }

  /**
   * 게시물을 삭제한다.
   *
   * <p>S3 이미지 → 댓글·대댓글 → 게시물 순으로 삭제합니다.
   * 소유자 본인만 삭제할 수 있습니다.</p>
   *
   * @param postId 삭제할 게시물 ID
   * @param user   요청 사용자 (소유권 검증에 사용)
   * @throws RingoException 게시물이 존재하지 않거나 소유자가 아닌 경우
   */
  @Transactional
  public void deletePost(Long postId, User user) {
    Post post = findPostOrThrow(postId);
    communityDomainService.validatePostOwnership(post, user);

    deleteAllPostImages(post);
    deleteAllPostComments(post);
    deleteAllPostLike(post);
    postRepository.delete(post);

    log.info("step=게시물_삭제_완료, postId={}, userId={}", postId, user.getId());
  }

  @Transactional
  void deleteAllPostLike(Post post){
    postLikeUserMappingRepository.deleteAllByPost(post);
  }

  /**
   * 추천 장소 기준으로 게시물 목록을 페이지 단위로 조회한다.
   *
   * <p>토픽이 {@code ENTIRE}이면 전체 게시물을, 그 외에는 해당 토픽의 게시물만 조회합니다.</p>
   *
   * @param topic            게시물 토픽 문자열 (예: "ENTIRE", "FOOD", "ACTIVITY" 등)
   * @param page             페이지 번호 (0부터 시작)
   * @param size             페이지당 게시물 수
   * @return 게시물 요약 목록 (이미지 포함)
   */
  public List<GetPostResponseDto> findPosts(User user, String topic, int page, int size) {
    PostCategory postCategory = GenericUtils.validateAndReturnEnumValue(PostCategory.values(), topic);
    Pageable pageable = PageRequest.of(page, size);

    Page<Post> posts = (postCategory == PostCategory.ENTIRE)
        ? postRepository.findAll(pageable)
        : postRepository.findByCategory(postCategory, pageable);

    return buildPostResponseDto(posts.stream().toList(), user);
  }

  public List<GetPostResponseDto> searchPostsByKeywordOrPlace(User user, String keyword, String place, int page, int size){
    String stripKeyword =null;
    if (keyword != null) stripKeyword = keyword.strip();

    List<Long> postIds = postSearchRepository.searchKeywordOrPlace(stripKeyword, place, PageRequest.of(page, size))
        .stream()
        .map(PostDocument::getId)
        .toList();

    List<Post> posts = postRepository.findAllByIdIn(postIds);

    return buildPostResponseDto(posts, user);
  }

  private List<GetPostResponseDto> buildPostResponseDto(Collection<Post> posts, User user){
    List<GetPostResponseDto> result = new ArrayList<>();
    for (Post post : posts){
      GetPostResponseDto dto = buildPostResponse(post, user);
      result.add(dto);
    }
    return result;
  }

  /**
   * 게시물에 댓글을 작성한다.
   *
   * @param dto  댓글 작성 요청 DTO (게시물 ID, 내용 포함)
   * @param user 댓글 작성자
   * @return 저장된 댓글 ID + 처리 결과 코드
   * @throws RingoException 게시물이 존재하지 않는 경우
   */
  @Transactional
  public CommentResponseDto createComment(CommentRequestDto dto, User user) {
    Post post = findPostOrThrow(dto.postId());
    postRepository.increaseCommentCount(dto.postId());
    Comment savedComment = commentRepository.save(Comment.of(post, user, dto.content()));
    eventPublisher.publish(new CommentCreatedEvent(savedComment.getId(), post.getId(), user.getId()));
    return new CommentResponseDto(savedComment.getId(), ErrorCode.SUCCESS.getCode());
  }

  /**
   * 댓글 내용을 수정한다.
   *
   * <p>댓글 작성자 본인만 수정할 수 있습니다.</p>
   *
   * @param commentId 수정할 댓글 ID
   * @param dto       수정할 내용이 담긴 DTO
   * @param user      요청 사용자
   * @throws RingoException 댓글이 존재하지 않거나 작성자가 아닌 경우
   */
  @Transactional
  public void updateComment(Long commentId, UpdateCommentRequestDto dto, User user) {
    Comment comment = findCommentOrThrow(commentId);
    communityDomainService.validateCommentOwnership(comment, user);
    comment.setContent(dto.content());
    commentRepository.save(comment);
  }

  /**
   * 댓글을 삭제한다.
   *
   * <p>댓글 삭제 전 해당 댓글에 달린 대댓글을 먼저 일괄 삭제합니다.
   * 댓글 작성자 본인만 삭제할 수 있습니다.</p>
   *
   * @param commentId 삭제할 댓글 ID
   * @param user      요청 사용자
   * @throws RingoException 작성자가 아닌 경우
   */
  @Transactional
  public void deleteComment(Long commentId, User user) {
    Comment comment = findCommentOrThrow(commentId);
    communityDomainService.validateCommentOwnership(comment, user);
    List<Comment> allCommentsUnderParent = findAllSubCommentsIncludeSelf(comment);
    postRepository.decreaseCommentCount(comment.getPost().getId(), allCommentsUnderParent.size());
    commentLikeUserMappingRepository.deleteAllByCommentIn(allCommentsUnderParent);
    commentRepository.deleteAll(allCommentsUnderParent);
  }

  private List<Comment> findAllSubCommentsIncludeSelf(Comment comment){
    List<Comment> result = new ArrayList<>();
    result.add(comment);
    List<Comment> subcomments = commentRepository.findAllByParentComment(comment);
    result.addAll(subcomments);
    return result;
  }

  /**
   * 게시물에 달린 댓글 목록을 조회한다.
   *
   * <p>각 댓글에 대댓글 목록이 포함되어 반환됩니다.</p>
   *
   * @param postId 조회할 게시물 ID
   * @return 댓글 목록 (대댓글 포함)
   * @throws RingoException 게시물이 존재하지 않는 경우
   */
  public List<GetCommentResponseDto> findCommentsByPost(Long postId, User user) {
    Post post = findPostOrThrow(postId);
    List<Comment> comments = commentRepository.findAllByPost(post);
    return buildCommentResponseDto(comments, user);
  }

  private List<GetCommentResponseDto> buildCommentResponseDto(List<Comment> comments, User user){
    List<GetCommentResponseDto> result = new ArrayList<>();
    Map<Long, GetCommentResponseDto> map = new HashMap<>();
    for (Comment comment : comments){
      if (comment.getParentComment() == null){
        GetCommentResponseDto response = createCommentResponseDto(comment, user);
        result.add(response);
        map.put(comment.getId(), response);
        continue;
      }
      GetCommentResponseDto parent = map.get(comment.getParentComment().getId());
      boolean isLike = commentLikeUserMappingRepository.existsByCommentAndUser(comment, user);
      parent.addSubCommentDto(comment, isLike);
    }
    return result;
  }

  private GetCommentResponseDto createCommentResponseDto(Comment comment, User user){
    boolean isLike = commentLikeUserMappingRepository.existsByCommentAndUser(comment, user);
    return GetCommentResponseDto.from(comment, isLike);
  }

  /**
   * 대댓글을 작성한다.
   *
   * @param dto  대댓글 작성 요청 DTO (부모 댓글 ID, 내용 포함)
   * @param user 대댓글 작성자
   * @return 저장된 대댓글 ID
   * @throws RingoException 부모 댓글이 존재하지 않는 경우
   */
  @Transactional
  public Long createSubComment(CreateSubCommentRequestDto dto, User user) {
    Comment comment = findCommentOrThrow(dto.commentId());
    validateCreateSubComment(comment);
    postRepository.increaseCommentCount(comment.getPost().getId());
    Comment savedComment = commentRepository.save(Comment.of(comment.getPost(), comment, user, dto.content()));
    return savedComment.getId();
  }

  private void validateCreateSubComment(Comment comment){
    if (comment.hasParent()){
      throw new RingoException("대댓글에 댓글을 달 수 없습니다.", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST);
    }
  }

  /**
   * 대댓글 내용을 수정한다.
   *
   * <p>대댓글 작성자 본인만 수정할 수 있습니다.</p>
   *
   * @param subCommentId 수정할 대댓글 ID
   * @param dto          수정할 내용이 담긴 DTO
   * @param user         요청 사용자
   * @throws RingoException 대댓글이 존재하지 않거나 작성자가 아닌 경우
   */
  @Transactional
  public void updateSubComment(Long subCommentId, UpdateSubCommentRequestDto dto, User user) {
    Comment subComment = findCommentOrThrow(subCommentId);
    communityDomainService.validateCommentOwnership(subComment, user);
    subComment.setContent(dto.content());
    commentRepository.save(subComment);
  }

  /**
   * 대댓글을 삭제한다.
   *
   * <p>대댓글 작성자 본인만 삭제할 수 있습니다.</p>
   *
   * @param subCommentId 삭제할 대댓글 ID
   * @param user         요청 사용자
   * @throws RingoException 작성자가 아닌 경우
   */
  @Transactional
  public void deleteSubComment(Long subCommentId, User user) {
    Comment subComment = findCommentOrThrow(subCommentId);
    communityDomainService.validateCommentOwnership(subComment, user);
    commentLikeUserMappingRepository.deleteAllByComment(subComment);
    commentRepository.delete(subComment);
  }

  /**
   * 게시물 좋아요를 토글한다.
   *
   * <p>이미 좋아요를 누른 상태이면 취소(매핑 삭제 + 카운트 감소)하고,
   * 아직 누르지 않은 상태이면 추가(매핑 저장 + 카운트 증가)합니다.</p>
   *
   * @param postId 좋아요를 토글할 게시물 ID
   * @param user   요청 사용자
   * @throws RingoException 게시물이 존재하지 않는 경우
   */
  @Transactional
  public void togglePostLike(Long postId, User user) {
    Post post = findPostOrThrow(postId);
    PostLikeUserMapping mapping = postLikeUserMappingRepository.findByPostAndUser(post, user);

    if (mapping != null && post.getLikeCount() > 0) {
      postRepository.decreasePostLikeCount(postId);
      postLikeUserMappingRepository.delete(mapping);
      return;
    }

    postRepository.increasePostLikeCount(postId);
    postLikeUserMappingRepository.save(PostLikeUserMapping.of(post, user));
  }

  /**
   * 댓글 좋아요를 토글한다.
   *
   * <p>이미 좋아요를 누른 상태이면 취소(매핑 삭제 + 카운트 감소)하고,
   * 아직 누르지 않은 상태이면 추가(매핑 저장 + 카운트 증가)합니다.</p>
   *
   * @param commentId 좋아요를 토글할 댓글 ID
   * @param user      요청 사용자
   * @throws RingoException 댓글이 존재하지 않는 경우
   */
  @Transactional
  public void toggleCommentLike(Long commentId, User user) {
    Comment comment = commentRepository.findById(commentId)
        .orElseThrow(() -> new RingoException("댓글 좋아요 처리 중 댓글을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
    CommentLikeUserMapping mapping = commentLikeUserMappingRepository.findByCommentAndUser(comment, user);

    if (mapping != null && comment.getLikeCount() > 0) {
      commentRepository.decreaseCommentLikeCount(commentId);
      commentLikeUserMappingRepository.delete(mapping);
      return;
    }

    commentRepository.increaseCommentLikeCount(commentId);
    commentLikeUserMappingRepository.save(CommentLikeUserMapping.of(comment, user));
  }

  // ─── private helpers ───────────────────────────────────────────────────────

  private User findUserOrThrow(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new RingoException("게시물을 포스팅하던 도중 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST));
  }

  private Comment findCommentOrThrow(Long commentId){
    return commentRepository.findById(commentId)
        .orElseThrow(() -> new RingoException("대댓글을 생성하던 도중 댓글을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
  }

  private Post findPostOrThrow(Long postId) {
    return postRepository.findById(postId)
        .orElseThrow(() -> new RingoException("게시물을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
  }

  /** 이미지 목록을 S3에 업로드하고 {@link PostImage} 엔티티로 저장한다. 부적절한 콘텐츠는 건너뜀. */
  @Transactional
  List<SavePostImageResponseDto> uploadAndSavePostImages(List<MultipartFile> images, Post post) {
    List<SavePostImageResponseDto> savedPostImages = new ArrayList<>();
    if (images == null) return savedPostImages;
    images.forEach(image -> {
      if (!imageService.containsInappropriateContent(image)) {
        String imageUrl = imageService.uploadImageToS3(image, "post");
        PostImage postImage = postImageRepository.save(PostImage.of(post, imageUrl));
        savedPostImages.add(new SavePostImageResponseDto(postImage.getId(), imageUrl));
      }
    });
    return savedPostImages;
  }

  /** 기존 S3 이미지를 삭제하고 새 이미지를 업로드하여 {@link PostImage} URL을 교체한다. */
  @Transactional
  UpdatePostImageResponseDto replacePostImage(UpdatePostImageRequestDto imageDto) {
    PostImage postImage = postImageRepository.findById(imageDto.imageId())
        .orElseThrow(() -> new RingoException("게시물 업데이트 중 해당 이미지를 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    String oldImageKey = imageService.extractS3ObjectKey(postImage.getImageUrl());
    imageService.deleteS3Object(oldImageKey);

    String newImageUrl = imageService.uploadImageToS3(imageDto.imageFile(), "post");
    postImage.setImageUrl(newImageUrl);
    postImageRepository.save(postImage);

    return new UpdatePostImageResponseDto(postImage.getId(), newImageUrl);
  }

  /** 게시물에 연결된 모든 이미지를 S3와 DB에서 삭제한다. */
  @Transactional
  void deleteAllPostImages(Post post) {
    postImageRepository.findAllByPost(post).forEach(postImage -> {
      String imageKey = imageService.extractS3ObjectKey(postImage.getImageUrl());
      imageService.deleteS3Object(imageKey);
      postImageRepository.delete(postImage);
    });
  }

  /** 게시물에 달린 모든 댓글과 대댓글을 삭제한다. */
  @Transactional
  void deleteAllPostComments(Post post) {
    List<Comment> comments = commentRepository.findAllByPost(post);
    commentLikeUserMappingRepository.deleteAllByCommentIn(comments);
    commentRepository.deleteAllByPost(post);
  }

  /** 게시물 엔티티와 이미지 DTO 목록으로 응답 DTO를 생성한다. */
  private GetPostResponseDto buildPostResponse(Post post, User user) {
    boolean isLike = postLikeUserMappingRepository.existsByPostAndUser(post, user);
    return GetPostResponseDto.from(post, isLike,  buildPostImageDtos(post));
  }

  /** 게시물에 연결된 이미지를 조회해 응답 DTO 목록으로 변환한다. */
  private List<GetPostImageResponseDto> buildPostImageDtos(Post post) {
    return postImageRepository.findAllByPost(post)
        .stream()
        .map(postImage -> new GetPostImageResponseDto(postImage.getId(), postImage.getImageUrl()))
        .toList();
  }

  @Transactional
  public void parseExcelAndPersistEntity(MultipartFile file){
    placeRepository.saveAll(parseExcelToSurveys(file));
  }

  private List<Place> parseExcelToSurveys(MultipartFile file) {
    List<Place> places = new ArrayList<>();
    try (InputStream inputStream = file.getInputStream();
        Workbook workbook = WorkbookFactory.create(inputStream)) {

      Sheet sheet = workbook.getSheetAt(0);
      sheet.removeRow(sheet.getRow(0)); // 헤더 행 제거

      for (Row row : sheet) {
        places.add(buildSurveyFromRow(row));
      }
    } catch (Exception e) {
      if (e instanceof RingoException re) throw re;
      log.error("step=설문_엑셀_파싱_실패, filename={}", file.getOriginalFilename(), e);
      throw new RingoException(e.getMessage(), ErrorCode.INTERNAL_SERVER_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    return places;
  }

  private Place buildSurveyFromRow(Row row) {
    return Place.builder()
        .name(row.getCell(0).getStringCellValue().strip())
        .category(categoryMap.get(row.getCell(1).getStringCellValue().strip()))
        .phoneNumber(row.getCell(3).getStringCellValue().strip())
        .detailAddress(row.getCell(4).getStringCellValue().strip())
        .description(row.getCell(5).getStringCellValue().strip())
        .keyword(modifyKeyword(row.getCell(7).getStringCellValue().strip()))
        .city("성남시")
        .district("분당구")
        .neighbor("판교동")
        .build();
  }

  private String modifyKeyword(String value){
    return String.join(",", value.split("\n"));
  }

  private static final Map<String, PostCategory> categoryMap = Map.of(
      "카페", PostCategory.CAFE,
      "맛집/술집", PostCategory.RESTAURANT,
      "놀거리", PostCategory.LEISURE
  );

  @Transactional
  public void updatePlaceClickCount(Long placeId){
    placeRepository.updatePlaceClickCount(placeId);
  }

  private Place findPlaceOrThrow(Long placeId){
    return placeRepository.findById(placeId)
        .orElseThrow(() -> new RingoException("장소 id를 찾을 수 없습니다.", ErrorCode.BAD_REQUEST, HttpStatus.BAD_REQUEST));
  }

  @Transactional
  public void scrapPlace(Long placeId, User user){
    Place place = findPlaceOrThrow(placeId);
    UserScrapPlace scrap = UserScrapPlace.of(place, user);
    userScrapPlaceRepository.save(scrap);
  }

  public List<GetPlaceDetailResponseDto> getScrappedPlace(User user){
    List<Place> userScrapPlaces = userScrapPlaceRepository.findAllByUser(user)
        .stream()
        .map(UserScrapPlace::getPlace)
        .toList();
    return userScrapPlaces.stream()
        .map(p -> p.createPlaceDetailDto(true))
        .toList();

  }
}
