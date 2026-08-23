package com.io.kira.application.chatbot.port.in;

import com.io.kira.application.chatbot.command.ChatbotCommand;
import com.io.kira.application.chatbot.error.ChatbotError;
import com.io.kira.application.chatbot.result.ChatbotReplyData;
import com.io.kira.common.result.Result;

public interface ChatbotUseCase {
    Result<ChatbotReplyData, ChatbotError> execute(ChatbotCommand command);
}
