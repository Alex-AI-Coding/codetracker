package com.io.kira.application.chatbot.result;

import java.util.UUID;

public record ChatbotReplyData(
        String reply,
        UUID threadId,
        String threadTitle,
        UUID classroomId
) {
}
