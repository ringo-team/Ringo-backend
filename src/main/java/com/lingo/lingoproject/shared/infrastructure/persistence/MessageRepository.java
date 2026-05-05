package com.lingo.lingoproject.shared.infrastructure.persistence;

import com.lingo.lingoproject.chat.application.ChatroomSummaryProjection;
import com.lingo.lingoproject.shared.domain.model.Message;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;


public interface MessageRepository extends MongoRepository<Message, String> {

  void deleteAllByChatroomId(Long chatroomId);

  @Query(value = "{ 'chatroomId': ?0, 'readerIds':  { $ne:  ?1 }, 'senderId':  { $ne:  ?1 }}", count = true)
  int findNumberOfUnreadMessages(Long chatroomId, Long userId);

  @Query(value = """
        {
        'chatroomId': ?0,
        'readerIds': { $ne: ?1 },
        'senderId': { $ne: ?1 }
        }"""
  )
  @Update(value = """
        { 
        '$addToSet': { 'readerIds': ?1 } 
        }"""
  )
  void readAllMessages(Long chatroomId, Long userId);

  Page<Message> findAllByChatroomIdOrderByCreatedAtDesc(Long chatroomId, Pageable pageable);

  Optional<Message> findFirstByChatroomIdOrderByCreatedAtDesc(Long chatroomId);

  @Aggregation(pipeline = {
      // 1. 대상 채팅방 필터
      "{ $match: { 'chatroomId': { $in: ?0 } } }",

      // 2. 채팅방별로 그룹핑
      """
      { $group: {
          '_id': '$chatroomId',
          'lastMessage': { $max: { 'createdAt': '$createdAt', 'content': '$content' } },
          'unreadCount': {
              $sum: {
                  $cond: [
                      { $and: [
                          { $ne: ['$senderId', ?1] },
                          { $not: { $in: [?1, '$readerIds'] } }
                      ]},
                      1, 0
                  ]
              }
          }
      }}
      """
  })
  List<ChatroomSummaryProjection> findChatroomSummaries(List<Long> chatroomIds, Long userId);
}
