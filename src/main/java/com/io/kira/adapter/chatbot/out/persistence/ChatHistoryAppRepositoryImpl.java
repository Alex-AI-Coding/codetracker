package com.io.kira.adapter.chatbot.out.persistence;

import com.io.kira.application.chatbot.port.out.ChatHistoryAppRepository;
import com.io.kira.application.chatbot.result.ChatHistoryMessageData;
import com.io.kira.application.chatbot.result.ChatMessagePageData;
import com.io.kira.application.chatbot.result.ChatThreadData;
import com.io.kira.application.chatbot.result.ChatThreadPageData;
import com.io.kira.domain.chatbot.valueobject.ChatMessageRole;
import com.io.kira.infrastructure.chatbot.persistence.entity.ChatMessageEntity;
import com.io.kira.infrastructure.chatbot.persistence.entity.ChatThreadEntity;
import com.io.kira.infrastructure.chatbot.persistence.repository.JpaChatMessageRepository;
import com.io.kira.infrastructure.chatbot.persistence.repository.JpaChatThreadRepository;
import com.io.kira.infrastructure.user.persistence.entity.UserEntity;
import com.io.kira.infrastructure.user.persistence.repository.JpaUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class ChatHistoryAppRepositoryImpl implements ChatHistoryAppRepository {

    private static final int MAX_PAGE_SIZE = 500;

    private final JpaChatThreadRepository threadRepository;
    private final JpaChatMessageRepository messageRepository;
    private final JpaUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ChatThreadData> findThread(UUID userId, UUID threadId) {
        return threadRepository.findByThreadIdAndUserEntity_UserId(threadId, userId)
                .map(this::toThreadData);
    }

    @Override
    @Transactional(readOnly = true)
    public ChatThreadPageData findThreads(UUID userId, int page, int size) {
        Slice<ChatThreadEntity> result = threadRepository
                .findByUserEntity_UserIdOrderByUpdatedAtDesc(
                        userId,
                        PageRequest.of(Math.max(0, page), normalizeLimit(size))
                );

        List<ChatThreadData> threads = result.stream()
                .map(this::toThreadData)
                .toList();
        return new ChatThreadPageData(
                threads,
                result.getNumber(),
                result.getSize(),
                result.hasNext()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatHistoryMessageData> findRecentMessages(UUID threadId, int limit) {
        List<ChatMessageEntity> newestFirst = new ArrayList<>(
                messageRepository.findByThreadEntity_ThreadIdOrderByPositionDesc(
                        threadId,
                        PageRequest.of(0, normalizeLimit(limit))
                )
        );
        Collections.reverse(newestFirst);
        return newestFirst.stream().map(this::toMessageData).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ChatMessagePageData findMessagePage(UUID threadId, Integer beforePosition, int size) {
        Slice<ChatMessageEntity> page = messageRepository.findMessagePage(
                threadId,
                beforePosition,
                PageRequest.of(0, normalizeLimit(size))
        );
        List<ChatMessageEntity> newestFirst = new ArrayList<>(page.getContent());
        Collections.reverse(newestFirst);
        List<ChatHistoryMessageData> messages = newestFirst.stream()
                .map(this::toMessageData)
                .toList();
        Integer nextBeforePosition = messages.isEmpty()
                ? null
                : messages.getFirst().position();
        return new ChatMessagePageData(messages, nextBeforePosition, page.hasNext());
    }

    @Override
    @Transactional
    public Optional<ChatThreadData> createThreadWithExchange(
            UUID userId,
            UUID classroomId,
            String title,
            String userMessage,
            String assistantMessage
    ) {
        Optional<UserEntity> user = userRepository.findById(userId);
        if (user.isEmpty()) {
            return Optional.empty();
        }

        Instant now = Instant.now();
        ChatThreadEntity thread = ChatThreadEntity.builder()
                .threadId(UUID.randomUUID())
                .userEntity(user.get())
                .classroomId(classroomId)
                .title(title)
                .createdAt(now)
                .updatedAt(now)
                .build();

        threadRepository.save(thread);
        saveExchange(thread, 1, userMessage, assistantMessage, now);
        return Optional.of(toThreadData(thread));
    }

    @Override
    @Transactional
    public Optional<ChatThreadData> appendExchange(
            UUID userId,
            UUID threadId,
            String userMessage,
            String assistantMessage
    ) {
        Optional<ChatThreadEntity> thread = threadRepository.findOwnedThreadForUpdate(threadId, userId);
        if (thread.isEmpty()) {
            return Optional.empty();
        }

        int nextPosition = messageRepository.findMaximumPosition(threadId) + 1;
        Instant now = Instant.now();
        saveExchange(thread.get(), nextPosition, userMessage, assistantMessage, now);
        thread.get().setUpdatedAt(now.plusMillis(1));
        threadRepository.save(thread.get());
        return Optional.of(toThreadData(thread.get()));
    }

    @Override
    @Transactional
    public Optional<ChatThreadData> renameThread(UUID userId, UUID threadId, String title) {
        Optional<ChatThreadEntity> thread = threadRepository.findOwnedThreadForUpdate(threadId, userId);
        if (thread.isEmpty()) {
            return Optional.empty();
        }

        thread.get().setTitle(title);
        thread.get().setUpdatedAt(Instant.now());
        threadRepository.save(thread.get());
        return Optional.of(toThreadData(thread.get()));
    }

    @Override
    @Transactional
    public boolean deleteThread(UUID userId, UUID threadId) {
        Optional<ChatThreadEntity> thread = threadRepository.findOwnedThreadForUpdate(threadId, userId);
        if (thread.isEmpty()) {
            return false;
        }

        messageRepository.deleteByThreadEntity_ThreadId(threadId);
        threadRepository.delete(thread.get());
        return true;
    }

    private void saveExchange(
            ChatThreadEntity thread,
            int userPosition,
            String userMessage,
            String assistantMessage,
            Instant createdAt
    ) {
        ChatMessageEntity user = ChatMessageEntity.builder()
                .messageId(UUID.randomUUID())
                .threadEntity(thread)
                .role(ChatMessageRole.USER)
                .content(userMessage)
                .position(userPosition)
                .createdAt(createdAt)
                .build();

        ChatMessageEntity assistant = ChatMessageEntity.builder()
                .messageId(UUID.randomUUID())
                .threadEntity(thread)
                .role(ChatMessageRole.ASSISTANT)
                .content(assistantMessage)
                .position(userPosition + 1)
                .createdAt(createdAt.plusMillis(1))
                .build();

        messageRepository.saveAll(List.of(user, assistant));
    }

    private int normalizeLimit(int limit) {
        return Math.max(1, Math.min(limit, MAX_PAGE_SIZE));
    }

    private ChatThreadData toThreadData(ChatThreadEntity entity) {
        return new ChatThreadData(
                entity.getThreadId(),
                entity.getClassroomId(),
                entity.getTitle(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private ChatHistoryMessageData toMessageData(ChatMessageEntity entity) {
        return new ChatHistoryMessageData(
                entity.getMessageId(),
                entity.getRole(),
                entity.getContent(),
                entity.getPosition(),
                entity.getCreatedAt()
        );
    }
}
