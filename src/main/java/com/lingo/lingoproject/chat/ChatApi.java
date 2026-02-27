package com.lingo.lingoproject.chat;

import com.lingo.lingoproject.chat.dto.CreateChatroomRequestDto;
import com.lingo.lingoproject.chat.dto.CreateChatroomResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatMessageResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatResponseDto;
import com.lingo.lingoproject.chat.dto.GetChatroomResponseDto;
import com.lingo.lingoproject.chat.dto.SaveAppointmentRequestDto;
import com.lingo.lingoproject.domain.User;
import com.lingo.lingoproject.utils.ApiListResponseDto;
import com.lingo.lingoproject.utils.ResultMessageResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name= "chat-controller", description = "채팅 관련 컨트롤러")
public interface ChatApi {
  /**
   * 메세지 내용 불러오는 api
   * paging 처리 default로 50개 메세지 전성
   *
   * 채팅창에 있는 메세지를 모두 읽음 처리한다.
   * 유저가 어떤 {roomId}에 접속하면 레디스에 connect::{userId}::{roomId} / true 를 저장해놓는다.
   */
  @Operation(
      summary = "채팅방 메세지 조회",
      description = "채팅방 메세지들을 조회하는 api"
  )
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "0000", description = "조회 성공", content = @Content(schema = @Schema(implementation =  GetChatResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "조회 권한이 없습니다.", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0004", description = "해당 id로 채팅방을 조회할 수 없습니다.", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation =  ResultMessageResponseDto.class)))
      }
  )
  @GetMapping("/chatrooms/{roomId}/messages")
  ResponseEntity<GetChatResponseDto> getChattingMessages(
      @Parameter(description = "채팅방 id", example = "4") @PathVariable(value = "roomId") Long roomId,
      @Parameter(description = "페이지 수", example = "2") @RequestParam(value = "page", defaultValue = "0") int page,
      @Parameter(description = "페이지 크기", example = "50") @RequestParam(value = "size", defaultValue = "100") int size,
      @AuthenticationPrincipal User user
  );

  @Operation(summary = "채팅방 생성", description = "채팅방과 채팅방 참여자 정보를 데이터베이스에 저장")
  @PostMapping("/chatrooms")
  ResponseEntity<CreateChatroomResponseDto> createChatRoom(
      @Valid @RequestBody CreateChatroomRequestDto dto);

  @Operation(summary = "채팅방 불러오기", description = "채팅방 목록 불러오기")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "0000", description = "조회 성공"),
          @ApiResponse(responseCode = "E0003", description = "채팅방 조회 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
      }
  )
  @GetMapping("/users/{userId}/chatrooms")
  ResponseEntity<ApiListResponseDto<GetChatroomResponseDto>> getChatrooms(
      @Parameter(description = "유저id", example = "5") @PathVariable(value = "userId") Long userId,
      @AuthenticationPrincipal User user
    );


  @Operation(summary = "채팅방 삭제", description = "채팅방, 채팅방 참여자 정보, 메세지 등을 삭제하는 api")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "0000", description = "삭제 성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0004", description = "해당 id로 채팅방을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "채팅방을 삭제할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
      }
  )
  @DeleteMapping("/chatrooms/{roomId}")
  ResponseEntity<ResultMessageResponseDto> deleteChatroom(
      @Parameter(description = "채팅방 id", example = "3") @PathVariable("roomId") Long roomId,
      @AuthenticationPrincipal User user
  );

  /**
   * 클라이언트가 메세지를 보낼 경로(/app/{roomId})를 적는다.
   * prefix인 app이 빠져있음
   */
  @MessageMapping("/{roomId}")
  void sendMessage(@DestinationVariable Long roomId, GetChatMessageResponseDto chatMessageDto);

  @PostMapping("/appointments")
  @ApiResponses(
      value = {
          @ApiResponse(responseCode = "0000", description = "성공", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0004", description = "해당 id로 채팅방을 찾을 수 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E0003", description = "약속을 등록할 권한이 없습니다.", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class))),
          @ApiResponse(responseCode = "E1000", description = "내부 오류, 기타 문의", content = @Content(schema = @Schema(implementation = ResultMessageResponseDto.class)))
      }
  )
  ResponseEntity<ResultMessageResponseDto> saveAppointment(@RequestBody SaveAppointmentRequestDto dto, @AuthenticationPrincipal User user);
}
