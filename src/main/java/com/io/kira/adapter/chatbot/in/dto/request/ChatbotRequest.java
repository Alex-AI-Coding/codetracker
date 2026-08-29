package com.io.kira.adapter.chatbot.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatbotRequest(
        @NotBlank(message = "Message is required")
        @Size(max = 2_000, message = "Message must not exceed 2000 characters")
        String message,
        UUID classroomId,
        UUID threadId
) {
}
