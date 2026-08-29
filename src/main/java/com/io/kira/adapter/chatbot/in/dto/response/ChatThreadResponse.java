package com.io.kira.adapter.chatbot.in.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.io.kira.application.chatbot.result.ChatThreadData;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatThreadResponse(
        ChatThreadData data,
        String message
) {
    public static ChatThreadResponse success(ChatThreadData data) {
        return new ChatThreadResponse(data, null);
    }

    public static ChatThreadResponse fail(String message) {
        return new ChatThreadResponse(null, message);
    }
}
