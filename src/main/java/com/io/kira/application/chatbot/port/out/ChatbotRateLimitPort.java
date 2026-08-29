package com.io.kira.application.chatbot.port.out;

import java.util.UUID;

public interface ChatbotRateLimitPort {
    boolean tryAcquire(UUID userId);
}
