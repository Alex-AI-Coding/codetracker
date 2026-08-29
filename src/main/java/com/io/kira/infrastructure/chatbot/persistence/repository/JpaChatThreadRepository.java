package com.io.kira.infrastructure.chatbot.persistence.repository;

import com.io.kira.infrastructure.chatbot.persistence.entity.ChatThreadEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaChatThreadRepository extends JpaRepository<ChatThreadEntity, UUID> {
    Optional<ChatThreadEntity> findByThreadIdAndUserEntity_UserId(UUID threadId, UUID userId);

    Slice<ChatThreadEntity> findByUserEntity_UserIdOrderByUpdatedAtDesc(UUID userId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT thread
            FROM ChatThreadEntity thread
            WHERE thread.threadId = :threadId
              AND thread.userEntity.userId = :userId
            """)
    Optional<ChatThreadEntity> findOwnedThreadForUpdate(
            @Param("threadId") UUID threadId,
            @Param("userId") UUID userId
    );
}
