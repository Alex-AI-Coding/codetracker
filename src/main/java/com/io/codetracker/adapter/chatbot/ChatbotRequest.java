package com.io.codetracker.adapter.chatbot;

import java.util.UUID;

public record ChatbotRequest(
        String message,
        UUID classroomId
) {
}
