package com.lingo.lingoproject.community.application;

import com.lingo.lingoproject.community.domain.event.PostCreatedEvent;
import com.lingo.lingoproject.community.presentation.dto.SavePostImageResponseDto;
import com.lingo.lingoproject.community.presentation.dto.SavePostRequestDto;
import com.lingo.lingoproject.community.presentation.dto.SavePostResponseDto;
import com.lingo.lingoproject.community.presentation.dto.UpdatePostImageResponseDto;
import com.lingo.lingoproject.community.presentation.dto.UpdatePostResponseDto;
import com.lingo.lingoproject.shared.domain.event.DomainEventPublisher;
import com.lingo.lingoproject.shared.domain.model.Comment;
import com.lingo.lingoproject.shared.domain.model.Post;
import com.lingo.lingoproject.shared.domain.model.PostCategory;
import com.lingo.lingoproject.shared.domain.model.PostImage;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.exception.ErrorCode;
import com.lingo.lingoproject.shared.infrastructure.persistence.CommentLikeUserMappingRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.CommentRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PostImageRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PostLikeUserMappingRepository;
import com.lingo.lingoproject.shared.infrastructure.persistence.PostRepository;
import com.lingo.lingoproject.shared.utils.GenericUtils;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CommunityUploadTransactionService {

  private final PostRepository postRepository;
  private final PostImageRepository postImageRepository;
  private final DomainEventPublisher domainEventPublisher;
  private final PostLikeUserMappingRepository postLikeUserMappingRepository;
  private final CommentRepository commentRepository;
  private final CommentLikeUserMappingRepository commentLikeUserMappingRepository;

  public CommunityUploadTransactionService(PostRepository postRepository,
      PostImageRepository postImageRepository, DomainEventPublisher domainEventPublisher,
      PostLikeUserMappingRepository postLikeUserMappingRepository,
      CommentRepository commentRepository,
      CommentLikeUserMappingRepository commentLikeUserMappingRepository) {
    this.postRepository = postRepository;
    this.postImageRepository = postImageRepository;
    this.domainEventPublisher = domainEventPublisher;
    this.postLikeUserMappingRepository = postLikeUserMappingRepository;
    this.commentRepository = commentRepository;
    this.commentLikeUserMappingRepository = commentLikeUserMappingRepository;
  }

  @Transactional
  public SavePostResponseDto savePostAndImages(SavePostRequestDto dto, User user, List<String> imageUrls){
    PostCategory postCategory = GenericUtils.validateAndReturnEnumValue(PostCategory.values(), dto.category());

    Post savedPost = postRepository.save(Post.of(user, dto.title(), dto.content(), postCategory));
    List<PostImage> postImages = imageUrls.stream().map(url -> PostImage.of(savedPost, url)).toList();
    List<PostImage> savedPostImages = postImageRepository.saveAll(postImages);

    List<SavePostImageResponseDto> imageResponse = savedPostImages.stream()
        .map(s -> new SavePostImageResponseDto(s.getId(), s.getImageUrl()))
        .toList();

    domainEventPublisher.publish(new PostCreatedEvent(savedPost));

    log.info("step=게시물_저장_완료, postId={}, userId={}, imageCount={}", savedPost.getId(), dto.userId(), savedPostImages.size());
    return new SavePostResponseDto(savedPost.getId(), imageResponse, ErrorCode.SUCCESS.getCode());
  }

  @Transactional
  public UpdatePostResponseDto updatePostAndImages(
      Post post,
      List<String> imageUrls,
      User user
  ){
    postImageRepository.deleteAllByPost(post);
    Post savedPost = postRepository.save(post);
    List<PostImage> postImages = imageUrls.stream().map(url -> PostImage.of(savedPost, url)).toList();
    List<UpdatePostImageResponseDto> updatedImages = postImageRepository.saveAll(postImages)
        .stream()
        .map(UpdatePostImageResponseDto::from)
        .toList();
    log.info("step=게시물_수정_완료, postId={}, userId={}", post.getId(), user.getId());

    return new UpdatePostResponseDto(updatedImages, ErrorCode.SUCCESS.getCode());
  }

  @Transactional
  public void deletePost(Post post) {
    deleteAllPostComments(post);
    postLikeUserMappingRepository.deleteAllByPost(post);
    postImageRepository.deleteAllByPost(post);
    postRepository.delete(post);
  }

  @Transactional
  void deleteAllPostComments(Post post) {
    List<Comment> comments = commentRepository.findAllByPost(post);
    commentLikeUserMappingRepository.deleteAllByCommentIn(comments);
    commentRepository.deleteAllByPost(post);
  }
}
