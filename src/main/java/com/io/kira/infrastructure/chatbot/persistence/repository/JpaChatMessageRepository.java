package com.io.kira.infrastructure.chatbot.persistence.repository;

import com.io.kira.infrastructure.chatbot.persistence.entity.ChatMessageEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    List<ChatMessageEntity> findByThreadEntity_ThreadIdOrderByPositionDesc(UUID threadId, Pageable pageable);

    @Query("""
            SELECT message
            FROM ChatMessageEntity message
            WHERE message.threadEntity.threadId = :threadId
              AND (:beforePosition IS NULL OR message.position < :beforePosition)
            ORDER BY message.position DESC
            """)
    Slice<ChatMessageEntity> findMessagePage(
            @Param("threadId") UUID threadId,
            @Param("beforePosition") Integer beforePosition,
            Pageable pageable
    );

    @Query("""
            SELECT COALESCE(MAX(message.position), 0)
            FROM ChatMessageEntity message
            WHERE message.threadEntity.threadId = :threadId
            """)
    int findMaximumPosition(@Param("threadId") UUID threadId);

    void deleteByThreadEntity_ThreadId(UUID threadId);
}
