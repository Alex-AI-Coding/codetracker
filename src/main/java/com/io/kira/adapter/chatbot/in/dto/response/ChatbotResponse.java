package com.io.kira.adapter.chatbot.in.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatbotResponse(
        String reply,
        String message,
        UUID threadId,
        String threadTitle,
        UUID classroomId
) {
    public static ChatbotResponse success(
            String reply,
            UUID threadId,
            String threadTitle,
            UUID classroomId
    ) {
        return new ChatbotResponse(reply, null, threadId, threadTitle, classroomId);
    }

    public static ChatbotResponse fail(String message) {
        return new ChatbotResponse(null, message, null, null, null);
    }
}
