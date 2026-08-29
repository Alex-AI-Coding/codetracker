package com.io.kira.application.chatbot.result;

import java.time.Instant;
import java.util.UUID;

public record ChatThreadData(
        UUID threadId,
        UUID classroomId,
        String title,
        Instant createdAt,
        Instant updatedAt
) {
}
