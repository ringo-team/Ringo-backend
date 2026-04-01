package com.lingo.lingoproject.community;

import com.lingo.lingoproject.community.dto.CommentRequestDto;
import com.lingo.lingoproject.community.dto.CommentResponseDto;
import com.lingo.lingoproject.community.dto.GetCommentResponseDto;
import com.lingo.lingoproject.community.dto.GetPostResponseDto;
import com.lingo.lingoproject.community.dto.SavePostImageResponseDto;
import com.lingo.lingoproject.community.dto.SavePostRequestDto;
import com.lingo.lingoproject.community.dto.SavePostResponseDto;
import com.lingo.lingoproject.community.dto.UpdateCommentRequestDto;
import com.lingo.lingoproject.community.dto.UpdatePostImageRequestDto;
import com.lingo.lingoproject.community.dto.UpdatePostImageResponseDto;
import com.lingo.lingoproject.community.dto.UpdatePostRequestDto;
import com.lingo.lingoproject.community.dto.UpdatePostResponseDto;
import com.lingo.lingoproject.domain.Comment;
import com.lingo.lingoproject.domain.CommentLikeUserMapping;
import com.lingo.lingoproject.domain.Post;
import com.lingo.lingoproject.domain.PostImageMapping;
import com.lingo.lingoproject.domain.PostLikeUserMapping;
import com.lingo.lingoproject.domain.Recommendation;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.domain.enums.PostTopic;
import com.lingo.lingoproject.exception.ErrorCode;
import com.lingo.lingoproject.exception.RingoException;
import com.lingo.lingoproject.image.ImageService;
import com.lingo.lingoproject.repository.CommentLikeUserMappingRepository;
import com.lingo.lingoproject.repository.CommentRepository;
import com.lingo.lingoproject.repository.PostImageMappingRepository;
import com.lingo.lingoproject.repository.PostLikeUserMappingRepository;
import com.lingo.lingoproject.repository.PostRepository;
import com.lingo.lingoproject.repository.RecommendationRepository;
import com.lingo.lingoproject.repository.UserRepository;
import com.lingo.lingoproject.utils.GenericUtils;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CommunityService {

  private final PostRepository postRepository;
  private final UserRepository userRepository;
  private final RecommendationRepository recommendationRepository;
  private final CommentRepository commentRepository;
  private final ImageService imageService;
  private final PostImageMappingRepository postImageMappingRepository;
  private final PostLikeUserMappingRepository postLikeUserMappingRepository;
  private final CommentLikeUserMappingRepository commentLikeUserMappingRepository;

  public SavePostResponseDto post(SavePostRequestDto dto, List<MultipartFile> images){
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

    List<SavePostImageResponseDto> imageList = new ArrayList<>();

    images.forEach(image -> {
      if (!imageService.isUnmoderateImage(image)) {
        // 사진 업로드
        String imageUrl = imageService.uploadImageToS3(image, "post");
        // db 사진 url 저장
        PostImageMapping mapping = PostImageMapping.builder()
            .post(savedPost)
            .imageUrl(imageUrl)
            .build();
        PostImageMapping savedMapping = postImageMappingRepository.save(mapping);
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
      throw new RingoException("게시자만 게시물을 업데이트할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }

    if (title != null && !title.isBlank()) post.setTitle(title);
    if (content != null && !content.isBlank()) post.setContent(content);
    if (postTopic != null) post.setTopic(postTopic);

    List<UpdatePostImageResponseDto> imageList = new ArrayList<>();

    images.forEach(image -> {
      PostImageMapping mapping = postImageMappingRepository.findById(image.imageId())
          .orElseThrow(() -> new RingoException("게시물 업데이트 중 해당이미지 사진을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));
      // s3에서 기존 사진 삭제
      String imageKey = imageService.getFilenameFromS3ImageUrl(mapping.getImageUrl());
      imageService.deleteImageInS3(imageKey);
      // s3에 새로운 사진 업로드
      String newImageUrl = imageService.uploadImageToS3(image.imageFile(), "post");
      // db에 사진 url 저장
      mapping.setImageUrl(newImageUrl);
      postImageMappingRepository.save(mapping);
      imageList.add(new UpdatePostImageResponseDto(mapping.getId(), newImageUrl));
    });

    postRepository.save(post);

    return new UpdatePostResponseDto(imageList, ErrorCode.SUCCESS.getCode());
  }

  public void deletePost(Long postId, User user){
    Post post = postRepository.findById(postId).orElseThrow(
        () -> new RingoException("업데이트할 게시물을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );

    Long authorId = post.getAuthor().getId();

    if (!authorId.equals(user.getId())){
      throw new RingoException("게시자만 게시물을 삭제할 수 있습니다.", ErrorCode.NO_AUTH, HttpStatus.BAD_REQUEST);
    }

    // s3 사진 삭제
    PostImageMapping mapping = postImageMappingRepository.findByPost(post);
    String imageKey = imageService.getFilenameFromS3ImageUrl(mapping.getImageUrl());
    imageService.deleteImageInS3(imageKey);

    commentRepository.deleteAllByPost(post);
    postImageMappingRepository.delete(mapping);
    postRepository.delete(post);
  }

  public List<GetPostResponseDto> getPost(Long recommendationId, String topic, int page, int size){

    Recommendation recommendation = recommendationRepository.findById(recommendationId)
        .orElseThrow(() -> new RingoException("해당 추천 장소가 존재하지 않습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST));

    Pageable pageable = PageRequest.of(page, size);
    Page<Post> posts = postRepository.findByRecommendation(recommendation, pageable);
    PostTopic postTopic = GenericUtils.validateAndReturnEnumValue(PostTopic.values(), topic);

    return posts.stream()
        .filter(post -> {
          if (postTopic == PostTopic.ENTIRE) return true;
          return postTopic == post.getTopic();
        })
        .map(
        post -> {
          PostImageMapping mapping = postImageMappingRepository.findByPost(post);
          return GetPostResponseDto.builder()
              .postId(post.getId())
              .title(post.getTitle())
              .content(post.getContent())
              .authorProfileUrl(post.getAuthor().getProfile().getImageUrl())
              .authorName(post.getAuthor().getNickname())
              .likeCount(post.getLikeCount())
              .commentCount(post.getCommentCount())
              .profileUrl(mapping.getImageUrl())
              .updatedAt(post.getUpdatedAt())
              .result(ErrorCode.SUCCESS.getCode())
              .build();
        }
    ).toList();
  }

  public CommentResponseDto comment(CommentRequestDto dto){
    Post post = postRepository.findById(dto.postId()).orElseThrow(
        () -> new RingoException("댓글 작성 중 게시물을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );
    User user = userRepository.findById(dto.userId()).orElseThrow(
        () -> new RingoException("유저를 찾을 수 없습니다.", ErrorCode.NOT_FOUND_USER, HttpStatus.BAD_REQUEST)
    );
    Comment comment = Comment.builder()
        .post(post)
        .user(user)
        .content(dto.content())
        .build();
    Comment savedComment = commentRepository.save(comment);
    return new CommentResponseDto(savedComment.getId(), ErrorCode.SUCCESS.getCode());
  }

  public void updateComment(UpdateCommentRequestDto dto, User user){
    Comment comment = commentRepository.findById(dto.commentId()).orElseThrow(
        () -> new RingoException("댓글 업데이트 도중 댓글을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );
    if (!comment.getUser().getId().equals(user.getId())){
      throw new RingoException("댓글을 수정할 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
    }
    comment.setContent(dto.content());
    commentRepository.save(comment);
  }

  public void deleteComment(Long commentId, User user){
    if(commentRepository.existsByIdAndUser(commentId, user)){
      commentRepository.deleteById(commentId);
      return;
    }
    throw new RingoException("댓글을 지울 권한이 없습니다.", ErrorCode.NO_AUTH, HttpStatus.FORBIDDEN);
  }

  public List<GetCommentResponseDto> getComments(Long postId){
    Post post = postRepository.findById(postId).orElseThrow(
        () -> new RingoException("댓글 조회 중 게시물을 찾을 수 없습니다.", ErrorCode.NOT_FOUND, HttpStatus.BAD_REQUEST)
    );
    return commentRepository.findAllByPost(post)
        .stream()
        .map(comment ->
            GetCommentResponseDto.builder()
                .commentId(comment.getId())
                .userProfileUrl(comment.getUser().getProfile().getImageUrl())
                .userName(comment.getUser().getName())
                .content(comment.getContent())
                .updatedAt(comment.getUpdatedAt())
                .result(ErrorCode.SUCCESS.getCode())
                .build()
        )
        .toList();
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
