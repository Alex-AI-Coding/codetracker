package com.io.kira.application.chatbot.port.out;

import com.io.kira.application.chatbot.error.ChatbotCompletionError;
import com.io.kira.application.chatbot.result.ChatHistoryMessageData;
import com.io.kira.common.result.Result;

import java.util.List;

public interface ChatbotCompletionPort {
    Result<String, ChatbotCompletionError> generateResponse(
            String userMessage,
            String accessLevel,
            String verifiedContext,
            List<ChatHistoryMessageData> history
    );
}
