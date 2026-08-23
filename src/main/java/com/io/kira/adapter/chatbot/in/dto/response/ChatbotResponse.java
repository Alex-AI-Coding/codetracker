package com.io.kira.adapter.chatbot.in.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatbotResponse(
        String reply,
        String message
) {
    public static ChatbotResponse success(String reply) {
        return new ChatbotResponse(reply, null);
    }

    public static ChatbotResponse fail(String message) {
        return new ChatbotResponse(null, message);
    }
}
