package com.io.kira.application.chatbot.result;

import com.io.kira.domain.chatbot.valueobject.ChatMessageRole;

import java.time.Instant;
import java.util.UUID;

public record ChatHistoryMessageData(
        UUID messageId,
        ChatMessageRole role,
        String content,
        int position,
        Instant createdAt
) {
}
