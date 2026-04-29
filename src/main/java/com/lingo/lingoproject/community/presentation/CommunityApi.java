package com.lingo.lingoproject.community.presentation;

import com.lingo.lingoproject.community.presentation.dto.CommentRequestDto;
import com.lingo.lingoproject.community.presentation.dto.CommentResponseDto;
import com.lingo.lingoproject.community.presentation.dto.CreateSubCommentRequestDto;
import com.lingo.lingoproject.community.presentation.dto.CreateSubCommentResponseDto;
import com.lingo.lingoproject.community.presentation.dto.GetCommentResponseDto;
import com.lingo.lingoproject.community.presentation.dto.GetPlaceDetailResponseDto;
import com.lingo.lingoproject.community.presentation.dto.GetPlaceResponseDto;
import com.lingo.lingoproject.community.presentation.dto.GetPostResponseDto;
import com.lingo.lingoproject.community.presentation.dto.SavePostRequestDto;
import com.lingo.lingoproject.community.presentation.dto.SavePostResponseDto;
import com.lingo.lingoproject.community.presentation.dto.ScrapPlaceRequestDto;
import com.lingo.lingoproject.community.presentation.dto.SearchPlaceRequestDto;
import com.lingo.lingoproject.community.presentation.dto.UpdateCommentRequestDto;
import com.lingo.lingoproject.community.presentation.dto.UpdatePostRequestDto;
import com.lingo.lingoproject.community.presentation.dto.UpdatePostResponseDto;
import com.lingo.lingoproject.community.presentation.dto.UpdateSubCommentRequestDto;
import com.lingo.lingoproject.shared.domain.model.User;
import com.lingo.lingoproject.shared.utils.ApiListResponseDto;
import com.lingo.lingoproject.shared.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Community API", description = "커뮤니티 기능 api")
public interface CommunityApi {
  @Operation(summary = "게시물 조회", description = "추천 장소에 해당하는 게시물을 조회하는 api")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0004", description = "해당 id의 추천 장소가 존재하지 않습니다.", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @GetMapping("/posts")
  ResponseEntity<List<GetPostResponseDto>> getPost(@RequestParam(value = "category") String category, @RequestParam(value = "page") int page, @RequestParam(value = "size") int size, @AuthenticationPrincipal User user);

  @Operation(summary = "게시물 업로드")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0005", description = "유저를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0004", description = "해당 id의 추천 장소가 존재하지 않습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @PostMapping("/posts")
  ResponseEntity<SavePostResponseDto> post(@Valid @RequestPart SavePostRequestDto dto, @RequestPart(value = "images", required = false) List<MultipartFile> images, @AuthenticationPrincipal User user);

  @Operation(summary = "게시물 삭제")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0004", description = "해당 id의 게시물을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "게시물을 업데이트할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @DeleteMapping("/posts/{post-id}")
  ResponseEntity<ResultMessageResponseDto> deletePost(@PathVariable(value = "post-id") Long postId, @AuthenticationPrincipal User user);

  @Operation(summary = "게시물 업데이트")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0005", description = "유저를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0004", description = "해당 id의 게시물을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "게시물을 업데이트할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @PatchMapping("/posts/{post-id}")
  ResponseEntity<UpdatePostResponseDto> updatePost(@PathVariable(value = "post-id") Long postId, @RequestBody UpdatePostRequestDto dto, @AuthenticationPrincipal User user);

  @Operation(summary = "댓글 조회")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0004", description = "해당 id의 게시물을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @GetMapping("/posts/{post-id}/comments")
  ResponseEntity<List<GetCommentResponseDto>> getComments(@PathVariable(value = "post-id") Long postId, @AuthenticationPrincipal User user);

  @Operation(summary = "댓글 업로드")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0004", description = "해당 id의 게시물이 존재하지 않습니다.", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0005", description = "유저를 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @PostMapping("/comments")
  ResponseEntity<CommentResponseDto> comment(@Valid @RequestBody CommentRequestDto dto, @AuthenticationPrincipal User user);


  @Operation(summary = "댓글 업데이트")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0004", description = "해당 id의 댓글을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "댓글을 업데이트할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @PatchMapping("/comments/{comment-id}")
  ResponseEntity<ResultMessageResponseDto> updateComment(@PathVariable(value = "comment-id") Long commentId, @Valid @RequestBody UpdateCommentRequestDto dto, @AuthenticationPrincipal User user);

  @Operation(summary = "댓글 삭제")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0003", description = "게시물을 삭제할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @DeleteMapping("/comments/{comment-id}")
  ResponseEntity<ResultMessageResponseDto> deleteComment(@PathVariable(value = "comment-id") Long commentId, @AuthenticationPrincipal User user);

  @Operation(summary = "대댓글 생성")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0004", description = "해당 id의 댓글이 존재하지 않습니다.", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @PostMapping("/sub-comments")
  ResponseEntity<CreateSubCommentResponseDto> createSubComment(@Valid @RequestBody CreateSubCommentRequestDto dto, @AuthenticationPrincipal User user);

  @Operation(summary = "대댓글 업데이트")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0004", description = "해당 id의 대댓글을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E0003", description = "대댓글을 업데이트할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @PatchMapping("/sub-comments/{sub-comment-id}")
  ResponseEntity<ResultMessageResponseDto> updateSubComment(@PathVariable(value = "sub-comment-id") Long subCommentId, @RequestBody UpdateSubCommentRequestDto dto, @AuthenticationPrincipal User user);

  @Operation(summary = "대댓글 삭제")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0003", description = "대댓글을 삭제할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @DeleteMapping("/sub-comments/{sub-comment-id}")
  ResponseEntity<ResultMessageResponseDto> deleteSubComment(@PathVariable(value = "sub-comment-id") Long subCommentId, @AuthenticationPrincipal User user);

  @Operation(summary = "게시물 좋아요")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0004", description = "게시물을 찾을 수 없습니다."),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @PatchMapping("/like/posts/{post-id}")
  ResponseEntity<ResultMessageResponseDto> likePost(@PathVariable("post-id") Long postId, @AuthenticationPrincipal User user);

  @Operation(summary = "댓글 좋아요")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "0000", description = "성공"),
      @ApiResponse(responseCode = "E0004", description = "댓글을 찾을 수 없습니다."),
      @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
  })
  @PatchMapping("/like/comments/{comment-id}")
  ResponseEntity<ResultMessageResponseDto> likeComment(@PathVariable("comment-id") Long commentId, @AuthenticationPrincipal User user);

  @Operation(summary = "장소/컨텐츠 관련 게시물 조회")
  @GetMapping("/posts/search")
  ResponseEntity<List<GetPostResponseDto>> getPlaceRelatedPost(@AuthenticationPrincipal User user, @RequestParam(required = false) String keyword, @RequestParam(required = false) String place, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);

  @PostMapping(value = "/places/excel", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  ResponseEntity<ResultMessageResponseDto> savePlace(@RequestParam("file") MultipartFile file);

  @GetMapping(value = "/places")
  ResponseEntity<GetPlaceResponseDto> getIndividualRecommendationPlaces(@AuthenticationPrincipal User user);

  @GetMapping(value = "/places/rank")
  ResponseEntity<List<GetPlaceDetailResponseDto>> getRankedPagedPlaces(@AuthenticationPrincipal User user, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size);

  @GetMapping(value = "/places/{place-id}")
  ResponseEntity<GetPlaceDetailResponseDto> getDetailPlaceInfo(@AuthenticationPrincipal User user, @PathVariable(value = "place-id") Long placeId);

  @PatchMapping(value = "/places/{place-id}")
  ResponseEntity<ResultMessageResponseDto> updatePlaceClickCount(@PathVariable(value = "place-id") Long placeId);

  @PostMapping(value = "/places/scrap")
  ResponseEntity<ResultMessageResponseDto> scrapPlace(@RequestBody ScrapPlaceRequestDto request, @AuthenticationPrincipal User user);

  @GetMapping(value = "/places/scrap")
  ResponseEntity<List<GetPlaceDetailResponseDto>> getScrappedPlace(@AuthenticationPrincipal User user);

  @GetMapping(value = "places/search")
  ResponseEntity<ApiListResponseDto<SearchPlaceRequestDto>> searchPlacesByKeywords(@RequestParam String keyword);
}
