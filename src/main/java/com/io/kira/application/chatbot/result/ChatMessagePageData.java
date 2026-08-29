package com.io.kira.application.chatbot.result;

import java.util.List;

public record ChatMessagePageData(
        List<ChatHistoryMessageData> messages,
        Integer nextBeforePosition,
        boolean hasMore
) {
}
