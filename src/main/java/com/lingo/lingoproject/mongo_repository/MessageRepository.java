package com.lingo.lingoproject.mongo_repository;

import com.lingo.lingoproject.domain.Message;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;


public interface MessageRepository extends MongoRepository<Message, String> {

  void deleteAllByChatroomId(Long chatroomId);

  @Query(value = "{ 'chatroomId': ?0, 'readerIds':  { $ne:  ?1 }, 'senderId':  { $ne:  ?1 }}", count = true)
  int findNumberOfNotReadMessages(Long chatroomId, Long userId);

  @Query(value = "{ 'chatroomId': ?0, 'readerIds':  { $ne:  ?1 }, 'senderId':  { $ne:  ?1 } }")
  List<Message> findNotReadMessages(Long chatroomId, Long userId);

  @Query(value = "{ '_id':  { $in:  ?0 } } ")
  @Update(value = "{ '$addToSet': { 'readerIds': ?1 } } ")
  void insertMemberIdInMessage(List<String> messageIds, Long userId);

  Page<Message> findAllByChatroomIdOrderByCreatedAtDesc(Long chatroomId, Pageable pageable);

  Optional<Message> findFirstByChatroomIdOrderByCreatedAtDesc(Long chatroomId);

}
