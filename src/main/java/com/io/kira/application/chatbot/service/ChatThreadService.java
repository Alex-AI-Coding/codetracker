package com.io.kira.application.chatbot.service;

import com.io.kira.application.chatbot.error.ChatThreadError;
import com.io.kira.application.chatbot.port.in.ChatThreadUseCase;
import com.io.kira.application.chatbot.port.out.ChatHistoryAppRepository;
import com.io.kira.application.chatbot.result.ChatMessagePageData;
import com.io.kira.application.chatbot.result.ChatThreadData;
import com.io.kira.application.chatbot.result.ChatThreadPageData;
import com.io.kira.common.result.Result;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class ChatThreadService implements ChatThreadUseCase {

    private static final int DEFAULT_MESSAGE_PAGE_SIZE = 100;
    private static final int MAX_MESSAGE_PAGE_SIZE = 200;
    private static final int MAX_TITLE_LENGTH = 100;
    private static final int DEFAULT_THREAD_PAGE_SIZE = 50;
    private static final int MAX_THREAD_PAGE_SIZE = 100;

    private final ChatHistoryAppRepository repository;

    public ChatThreadService(ChatHistoryAppRepository repository) {
        this.repository = repository;
    }

    @Override
    public ChatThreadPageData listThreads(UUID userId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = size <= 0
                ? DEFAULT_THREAD_PAGE_SIZE
                : Math.min(size, MAX_THREAD_PAGE_SIZE);
        return repository.findThreads(userId, safePage, safeSize);
    }

    @Override
    public Result<ChatMessagePageData, ChatThreadError> getMessages(
            UUID userId,
            UUID threadId,
            Integer beforePosition,
            int size
    ) {
        if (repository.findThread(userId, threadId).isEmpty()) {
            return Result.fail(ChatThreadError.THREAD_NOT_FOUND);
        }

        int safeSize = size <= 0
                ? DEFAULT_MESSAGE_PAGE_SIZE
                : Math.min(size, MAX_MESSAGE_PAGE_SIZE);
        Integer safeBefore = beforePosition != null && beforePosition > 0
                ? beforePosition
                : null;
        return Result.ok(repository.findMessagePage(threadId, safeBefore, safeSize));
    }

    @Override
    public Result<ChatThreadData, ChatThreadError> renameThread(UUID userId, UUID threadId, String title) {
        String normalizedTitle = normalizeTitle(title);
        if (normalizedTitle == null) {
            return Result.fail(ChatThreadError.INVALID_TITLE);
        }

        Optional<ChatThreadData> updated = repository.renameThread(userId, threadId, normalizedTitle);
        if (updated.isEmpty()) {
            return Result.fail(ChatThreadError.THREAD_NOT_FOUND);
        }
        return Result.ok(updated.get());
    }

    @Override
    public Result<Void, ChatThreadError> deleteThread(UUID userId, UUID threadId) {
        return repository.deleteThread(userId, threadId)
                ? Result.ok(null)
                : Result.fail(ChatThreadError.THREAD_NOT_FOUND);
    }

    private String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }

        String normalized = title.replaceAll("\\s+", " ").trim();
        if (normalized.isBlank() || normalized.length() > MAX_TITLE_LENGTH) {
            return null;
        }
        return normalized;
    }
}
