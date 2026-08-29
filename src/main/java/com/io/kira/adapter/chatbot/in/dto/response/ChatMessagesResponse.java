package com.io.kira.adapter.chatbot.in.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.io.kira.application.chatbot.result.ChatHistoryMessageData;
import com.io.kira.application.chatbot.result.ChatMessagePageData;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ChatMessagesResponse(
        List<ChatHistoryMessageData> data,
        Integer nextBeforePosition,
        Boolean hasMore,
        String message
) {
    public static ChatMessagesResponse success(ChatMessagePageData page) {
        return new ChatMessagesResponse(
                page.messages(),
                page.nextBeforePosition(),
                page.hasMore(),
                null
        );
    }

    public static ChatMessagesResponse fail(String message) {
        return new ChatMessagesResponse(null, null, null, message);
    }
}
