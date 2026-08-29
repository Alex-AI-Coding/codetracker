package com.io.kira.adapter.chatbot.in.dto.response;

import com.io.kira.application.chatbot.result.ChatThreadData;
import com.io.kira.application.chatbot.result.ChatThreadPageData;

import java.util.List;

public record ChatThreadsResponse(
        List<ChatThreadData> data,
        int page,
        int size,
        boolean hasMore
) {
    public static ChatThreadsResponse from(ChatThreadPageData page) {
        return new ChatThreadsResponse(
                page.threads(),
                page.page(),
                page.size(),
                page.hasMore()
        );
    }
}
