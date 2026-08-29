package com.io.kira.application.chatbot.result;

import java.util.List;

public record ChatThreadPageData(
        List<ChatThreadData> threads,
        int page,
        int size,
        boolean hasMore
) {
}
