package com.io.kira.application.chatbot.port.in;

import com.io.kira.application.chatbot.error.ChatThreadError;
import com.io.kira.application.chatbot.result.ChatMessagePageData;
import com.io.kira.application.chatbot.result.ChatThreadData;
import com.io.kira.application.chatbot.result.ChatThreadPageData;
import com.io.kira.common.result.Result;

import java.util.UUID;

public interface ChatThreadUseCase {
    ChatThreadPageData listThreads(UUID userId, int page, int size);

    Result<ChatMessagePageData, ChatThreadError> getMessages(
            UUID userId,
            UUID threadId,
            Integer beforePosition,
            int size
    );

    Result<ChatThreadData, ChatThreadError> renameThread(UUID userId, UUID threadId, String title);

    Result<Void, ChatThreadError> deleteThread(UUID userId, UUID threadId);
}
