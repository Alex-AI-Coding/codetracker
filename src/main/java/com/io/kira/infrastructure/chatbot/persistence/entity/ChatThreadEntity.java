package com.io.kira.infrastructure.chatbot.persistence.entity;

import com.io.kira.infrastructure.user.persistence.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "chat_thread",
        indexes = {
                @Index(name = "idx_chat_thread_user_updated", columnList = "user_id, updated_at"),
                @Index(name = "idx_chat_thread_classroom", columnList = "classroom_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatThreadEntity {

    @Id
    @Column(name = "thread_id", nullable = false, updatable = false)
    private UUID threadId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    private UserEntity userEntity;

    @Column(name = "classroom_id", updatable = false)
    private UUID classroomId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }
}
