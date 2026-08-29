package com.io.kira.application.chatbot.port.out;

import com.io.kira.application.chatbot.result.ChatHistoryMessageData;
import com.io.kira.application.chatbot.result.ChatMessagePageData;
import com.io.kira.application.chatbot.result.ChatThreadData;
import com.io.kira.application.chatbot.result.ChatThreadPageData;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatHistoryAppRepository {
    Optional<ChatThreadData> findThread(UUID userId, UUID threadId);

    ChatThreadPageData findThreads(UUID userId, int page, int size);

    List<ChatHistoryMessageData> findRecentMessages(UUID threadId, int limit);

    ChatMessagePageData findMessagePage(UUID threadId, Integer beforePosition, int size);

    Optional<ChatThreadData> createThreadWithExchange(
            UUID userId,
            UUID classroomId,
            String title,
            String userMessage,
            String assistantMessage
    );

    Optional<ChatThreadData> appendExchange(
            UUID userId,
            UUID threadId,
            String userMessage,
            String assistantMessage
    );

    Optional<ChatThreadData> renameThread(UUID userId, UUID threadId, String title);

    boolean deleteThread(UUID userId, UUID threadId);
}
