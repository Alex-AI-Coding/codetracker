package com.io.kira.application.chatbot.command;

import java.util.UUID;

public record ChatbotCommand(
        UUID userId,
        String message,
        UUID classroomId
) {
}
