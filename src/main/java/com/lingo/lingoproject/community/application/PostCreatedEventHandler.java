package com.lingo.lingoproject.community.application;

import com.lingo.lingoproject.community.domain.event.PostCreatedEvent;
import com.lingo.lingoproject.shared.domain.elastic.PostDocument;
import com.lingo.lingoproject.shared.infrastructure.elastic.PostSearchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostCreatedEventHandler {
  private final PostSearchRepository postSearchRepository;

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  @Async
  public void handle(PostCreatedEvent event){
    PostDocument post = PostDocument.from(event);

    log.info("post_document가 저장됨 id={}", post.getId());

    postSearchRepository.save(post);
  }
}
