package com.lingo.lingoproject.api.community;

import com.lingo.lingoproject.api.community.dto.CommentRequestDto;
import com.lingo.lingoproject.api.community.dto.CommentResponseDto;
import com.lingo.lingoproject.api.community.dto.CreateSubCommentRequestDto;
import com.lingo.lingoproject.api.community.dto.GetCommentResponseDto;
import com.lingo.lingoproject.api.community.dto.GetPostImageResponseDto;
import com.lingo.lingoproject.api.community.dto.GetPostResponseDto;
import com.lingo.lingoproject.api.community.dto.GetSubCommentResponseDto;
import com.lingo.lingoproject.api.community.dto.SavePostImageResponseDto;
import com.lingo.lingoproject.api.community.dto.SavePostRequestDto;
import com.lingo.lingoproject.api.community.dto.SavePostResponseDto;
import com.lingo.lingoproject.api.community.dto.UpdateCommentRequestDto;
import com.lingo.lingoproject.api.community.dto.UpdatePostImageRequestDto;
import com.lingo.lingoproject.api.community.dto.UpdatePostImageResponseDto;
import com.lingo.lingoproject.api.community.dto.UpdatePostRequestDto;
import com.lingo.lingoproject.api.community.dto.UpdatePostResponseDto;
import com.lingo.lingoproject.api.community.dto.UpdateSubCommentRequestDto;
import com.lingo.lingoproject.db.domain.Comment;
import com.lingo.lingoproject.db.domain.CommentLikeUserMapping;
import com.lingo.lingoproject.db.domain.Post;
import com.lingo.lingoproject.db.domain.PostImage;
import com.lingo.lingoproject.db.domain.PostLikeUserMapping;
import com.lingo.lingoproject.db.domain.Recommendation;
import com.lingo.lingoproject.db.domain.SubComment;
import com.lingo.lingoproject.db.domain.User;
import com.lingo.lingoproject.db.domain.enums.PostTopic;
import com.lingo.lingoproject.common.exception.ErrorCode;
import com.lingo.lingoproject.common.exception.RingoException;
import com.lingo.lingoproject.api.image.ImageService;
import com.lingo.lingoproject.db.repository.CommentLikeUserMappingRepository;
import com.lingo.lingoproject.db.repository.CommentRepository;
import com.lingo.lingoproject.db.repository.PostImageRepository;
import com.lingo.lingoproject.db.repository.PostLikeUserMappingRepository;
import com.lingo.lingoproject.db.repository.PostRepository;
import com.lingo.lingoproject.db.repository.RecommendationRepository;
import com.lingo.lingoproject.db.repository.SubCommentRepository;
import com.lingo.lingoproject.db.repository.UserRepository;
import com.lingo.lingoproject.common.utils.GenericUtils;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final RecommendationRepository recommendationRepository;
  private final CommentRepository commentRepository;
  private final ImageService imageService;
  private final PostImageRepository postImageRepository;
  private final PostLikeUserMappingRepository postLikeUserMappingRepository;
  private final CommentLikeUserMappingRepository commentLikeUserMappingRepository;
  private final SubCommentRepository subCommentRepository;

  public SavePostResponseDto post(SavePostRequestDto dto, List<MultipartFile> images){
    log.info("""

        step=게시물_작성_시작,
        userId={},
        recommendationId={},
        topic={}

        """, dto.userId(), dto.recommendationId(), dto.topic());
    User author = userRepository.findById(dto.userId()).orElseThrow(
        () -> new RingoException("게시물을 포스팅하던 도중 유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST)
    );
    Recommendation recommendation = recommendationRepository.findById(dto.recommendationId())
        .orElseThrow(() -> new RingoException("해당 추천 장소가 존재하지 않습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
    PostTopic postTopic = GenericUtils.validateAndReturnEnumValue(PostTopic.values(), dto.topic());

    Post post = Post.builder()
        .author(author)
        .recommendation(recommendation)
        .title(dto.title())
        .content(dto.content())
        .topic(postTopic)
        .build();
    Post savedPost = postRepository.save(post);
    log.info("""

        step=게시물_저장_완료,
        postId={},
        userId={},
        imageCount={}

        """, savedPost.getId(), dto.userId(), images.size());

    List<SavePostImageResponseDto> imageList = new ArrayList<>();

    images.forEach(image -> {
      if (!imageService.isUnmoderateImage(image)) {
        // 사진 업로드
        String imageUrl = imageService.uploadImageToS3(image, "post");
        // db 사진 url 저장
        PostImage mapping = PostImage.builder()
            .post(savedPost)
            .imageUrl(imageUrl)
            .build();
        PostImage savedMapping = postImageRepository.save(mapping);
        // 리턴값에 id와 사진url 전달
        imageList.add(new SavePostImageResponseDto(savedMapping.getId(), imageUrl));
      }
    });

    return new SavePostResponseDto(savedPost.getId(), imageList, ErrorCode.SUCCESS.getCode());
  }

  public UpdatePostResponseDto updatePost(Long postId, UpdatePostRequestDto dto, User user){
    Post post = postRepository.findById(postId).orElseThrow(
        () -> new RingoException("업데이트할 게시물을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );

    PostTopic postTopic = dto.topic() != null ? GenericUtils.validateAndReturnEnumValue(PostTopic.values(), dto.topic()) : null;
    Long authorId = post.getAuthor().getId();
    String title = dto.title();
    String content = dto.content();
    List<UpdatePostImageRequestDto> images = dto.imagelist();

    if (!authorId.equals(user.getId())){
      log.warn("""

          step=게시물_수정_권한없음,
          postId={},
          requestUserId={},
          authorId={}

          """, postId, user.getId(), authorId);
      throw new RingoException("게시자만 게시물을 업데이트할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    if (title != null && !title.isBlank()) post.setTitle(title);
    if (content != null && !content.isBlank()) post.setContent(content);
    if (postTopic != null) post.setTopic(postTopic);

    List<UpdatePostImageResponseDto> imageList = new ArrayList<>();

    images.forEach(image -> {
      PostImage mapping = postImageRepository.findById(image.imageId())
          .orElseThrow(() -> new RingoException("게시물 업데이트 중 해당이미지 사진을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
      // s3에서 기존 사진 삭제
      String imageKey = imageService.getFilenameFromS3ImageUrl(mapping.getImageUrl());
      imageService.deleteImageInS3(imageKey);
      // s3에 새로운 사진 업로드
      String newImageUrl = imageService.uploadImageToS3(image.imageFile(), "post");
      // db에 사진 url 저장
      mapping.setImageUrl(newImageUrl);
      postImageRepository.save(mapping);
      imageList.add(new UpdatePostImageResponseDto(mapping.getId(), newImageUrl));
    });

    postRepository.save(post);
    log.info("""

        step=게시물_수정_완료,
        postId={},
        userId={}

        """, postId, user.getId());

    return new UpdatePostResponseDto(imageList, ErrorCode.SUCCESS.getCode());
  }

  public void deletePost(Long postId, User user){
    Post post = postRepository.findById(postId).orElseThrow(
        () -> new RingoException("업데이트할 게시물을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );

    Long authorId = post.getAuthor().getId();

    if (!authorId.equals(user.getId())){
      log.warn("""

          step=게시물_삭제_권한없음,
          postId={},
          requestUserId={},
          authorId={}

          """, postId, user.getId(), authorId);
      throw new RingoException("게시자만 게시물을 삭제할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

    // s3 사진 삭제
    postImageRepository.findAllByPost(post).forEach(mapping -> {
          String imageKey = imageService.getFilenameFromS3ImageUrl(mapping.getImageUrl());
          imageService.deleteImageInS3(imageKey);

          postImageRepository.delete(mapping);
        }
    );
    commentRepository.findAllByPost(post).forEach(comment ->{
      // 대댓글 삭제
      subCommentRepository.deleteAllByComment(comment);
      // 댓글 삭제
      commentRepository.delete(comment);
    });
    postRepository.delete(post);
    log.info("""

        step=게시물_삭제_완료,
        postId={},
        userId={}

        """, postId, user.getId());
  }

  public List<GetPostResponseDto> getPost(Long recommendationId, String topic, int page, int size){

    Recommendation recommendation = recommendationRepository.findById(recommendationId)
        .orElseThrow(() -> new RingoException("해당 추천 장소가 존재하지 않습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    // topic 검증
    PostTopic postTopic = GenericUtils.validateAndReturnEnumValue(PostTopic.values(), topic);

    Pageable pageable = PageRequest.of(page, size);
    Page<Post> posts;

    if (postTopic == PostTopic.ENTIRE) posts = postRepository.findByRecommendation(recommendation, pageable);
    else posts = postRepository.findByRecommendationAndTopic(recommendation, postTopic, pageable);

    return posts.stream()
        .map(
        post -> {
          // 해당 게시글의 이미지 조회
          List<GetPostImageResponseDto> images = postImageRepository.findAllByPost(post)
              .stream()
              .map(postImage ->
                  new GetPostImageResponseDto(postImage.getId(), postImage.getImageUrl())
              )
              .toList();
          // 응답 dto 생성
          return GetPostResponseDto.builder()
              .postId(post.getId())
              .title(post.getTitle())
              .content(post.getContent())
              .authorProfileUrl(post.getAuthor().getProfile().getImageUrl())
              .authorName(post.getAuthor().getNickname())
              .likeCount(post.getLikeCount())
              .commentCount(post.getCommentCount())
              .images(images)
              .updatedAt(post.getUpdatedAt())
              .result(ErrorCode.SUCCESS.getCode())
              .build();
        }
    ).toList();
  }

  public CommentResponseDto comment(CommentRequestDto dto, User user){
    Post post = postRepository.findById(dto.postId()).orElseThrow(
        () -> new RingoException("댓글 작성 중 게시물을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );
    Comment comment = Comment.builder()
        .post(post)
        .user(user)
        .content(dto.content())
        .build();
    Comment savedComment = commentRepository.save(comment);
    return new CommentResponseDto(savedComment.getId(), ErrorCode.SUCCESS.getCode());
  }

  public void updateComment(Long commentId, UpdateCommentRequestDto dto, User user){
    Comment comment = commentRepository.findById(commentId).orElseThrow(
        () -> new RingoException("댓글 업데이트 도중 댓글을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );
    if (!comment.getUser().getId().equals(user.getId())){
      log.warn("""

          step=댓글_수정_권한없음,
          commentId={},
          requestUserId={},
          commentOwnerId={}

          """, commentId, user.getId(), comment.getUser().getId());
      throw new RingoException("댓글을 수정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
    comment.setContent(dto.content());
    commentRepository.save(comment);
  }

  public void deleteComment(Long commentId, User user){
    if(commentRepository.existsByIdAndUser(commentId, user)){
      Comment comment = commentRepository.findById(commentId).orElse(null);
      // 대댓글 삭제
      subCommentRepository.deleteAllByComment(comment);
      // 댓글 삭제
      commentRepository.deleteById(commentId);
      return;
    }
    log.warn("""

        step=댓글_삭제_권한없음,
        commentId={},
        requestUserId={}

        """, commentId, user.getId());
    throw new RingoException("댓글을 지울 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
  }

  public List<GetCommentResponseDto> getComments(Long postId){
    Post post = postRepository.findById(postId).orElseThrow(
        () -> new RingoException("댓글 조회 중 게시물을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );
    return commentRepository.findAllByPost(post)
        .stream()
        .map(comment -> {
          List<GetSubCommentResponseDto> subComments = subCommentRepository.findAllByComment(
                  comment)
              .stream()
              .map(subComment -> GetSubCommentResponseDto.builder()
                  .subCommentId(subComment.getId())
                  .content(subComment.getContent())
                  .userId(subComment.getUser().getId())
                  .userNickname(subComment.getUser().getNickname())
                  .userProfileUrl(subComment.getUser().getProfile().getImageUrl())
                  .updatedAt(subComment.getUpdatedAt().toString())
                  .build()
              )
              .toList();

          return GetCommentResponseDto.builder()
                .commentId(comment.getId())
                .userId(comment.getUser().getId())
                .userProfileUrl(comment.getUser().getProfile().getImageUrl())
                .userNickname(comment.getUser().getNickname())
                .subComments(subComments)
                .content(comment.getContent())
                .updatedAt(comment.getUpdatedAt())
                .result(ErrorCode.SUCCESS.getCode())
                .build();
            }
        )
        .toList();
  }

  public Long createSubComment(CreateSubCommentRequestDto dto, User user) {
    Comment comment = commentRepository.findById(dto.commentId()).orElseThrow(
        () -> new RingoException("대댓글을 생성하던 도중 댓글을 찾을 수 없습니다.", ErrorCode.NOT_FOUND,
            HttpStatus.BAD_REQUEST)
    );
    return subCommentRepository.save(
        SubComment.builder()
            .user(user)
            .comment(comment)
            .content(dto.content())
            .build()
    ).getId();
  }

  public void updateSubComment(Long subCommentId, UpdateSubCommentRequestDto dto, User user){
    SubComment subComment = subCommentRepository.findById(subCommentId).orElseThrow(
        () -> new RingoException("대댓글을 업데이트하던 도중 대댓글을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );
    if (subComment.getUser().getId().equals(user.getId())){
      subComment.setContent(dto.content());
      subCommentRepository.save(subComment);
    }
    else {
      log.warn("""

          step=대댓글_수정_권한없음,
          subCommentId={},
          requestUserId={},
          subCommentOwnerId={}

          """, subCommentId, user.getId(), subComment.getUser().getId());
      throw new RingoException("대댓글을 수정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
  }

  public void deleteSubComment(Long subCommentId, User user){
    if (subCommentRepository.existsByIdAndUser(subCommentId, user)){
      subCommentRepository.deleteById(subCommentId);
    }
    else {
      log.warn("""

          step=대댓글_삭제_권한없음,
          subCommentId={},
          requestUserId={}

          """, subCommentId, user.getId());
      throw new RingoException("대댓글을 삭제할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
  }

  public void likePost(Long postId, User user){
    Post post = postRepository.findById(postId).orElseThrow(
        () -> new RingoException("게시물 좋아요 처리 중 게시물을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );
    PostLikeUserMapping mapping = postLikeUserMappingRepository.findByPostAndUser(post, user);

    if(mapping != null && post.getLikeCount() > 0){
      postRepository.decreasePostLikeCount(postId);
      postLikeUserMappingRepository.delete(mapping);
      return;
    }

    postRepository.increasePostLikeCount(postId);
    postLikeUserMappingRepository.save(PostLikeUserMapping.builder()
        .post(post)
        .user(user)
        .build()
    );
  }

  public void likeComment(Long commentId, User user){
    Comment comment = commentRepository.findById(commentId).orElseThrow(
        () -> new RingoException("댓글 좋아요 처리 중 댓글을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );

    CommentLikeUserMapping mapping = commentLikeUserMappingRepository.findByCommentAndUser(comment, user);

    if (mapping != null && comment.getLikeCount() > 0){
      commentRepository.decreaseCommentLikeCount(commentId);
      commentLikeUserMappingRepository.delete(mapping);
    }

    commentRepository.increaseCommentLikeCount(commentId);
    commentLikeUserMappingRepository.save(
        CommentLikeUserMapping.builder()
            .comment(comment)
            .user(user)
            .build()
    );
}

}
