package com.io.kira.adapter.chatbot.in.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameChatThreadRequest(
        @NotBlank(message = "Thread title is required")
        @Size(max = 100, message = "Thread title must not exceed 100 characters")
        String title
) {
}
