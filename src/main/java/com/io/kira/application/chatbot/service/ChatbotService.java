package com.io.kira.application.chatbot.service;

import com.io.kira.application.chatbot.command.ChatbotCommand;
import com.io.kira.application.chatbot.error.ChatbotCompletionError;
import com.io.kira.application.chatbot.error.ChatbotError;
import com.io.kira.application.chatbot.port.in.ChatbotUseCase;
import com.io.kira.application.chatbot.port.out.ChatbotCompletionPort;
import com.io.kira.application.chatbot.port.out.ChatHistoryAppRepository;
import com.io.kira.application.chatbot.port.out.ChatbotRateLimitPort;
import com.io.kira.application.chatbot.result.ChatHistoryMessageData;
import com.io.kira.application.chatbot.result.ChatbotReplyData;
import com.io.kira.application.chatbot.result.ChatThreadData;
import com.io.kira.common.result.Result;
import com.io.kira.domain.chatbot.valueobject.ChatMessageRole;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ChatbotService implements ChatbotUseCase {

    private static final int MAX_HISTORY_MESSAGES_FOR_MODEL = 20;
    private static final int MAX_HISTORY_CHARACTERS_FOR_MODEL = 40_000;
    private static final int MAX_ASSISTANT_REPLY_CHARACTERS = 12_000;
    private static final int MAX_THREAD_TITLE_CHARACTERS = 72;

    private final ChatbotAccessService accessService;
    private final ChatbotContextService contextService;
    private final ChatbotCompletionPort completionPort;
    private final ChatHistoryAppRepository historyRepository;
    private final ChatbotRateLimitPort rateLimitPort;

    public ChatbotService(
            ChatbotAccessService accessService,
            ChatbotContextService contextService,
            ChatbotCompletionPort completionPort,
            ChatHistoryAppRepository historyRepository,
            ChatbotRateLimitPort rateLimitPort
    ) {
        this.accessService = accessService;
        this.contextService = contextService;
        this.completionPort = completionPort;
        this.historyRepository = historyRepository;
        this.rateLimitPort = rateLimitPort;
    }

    @Override
    public Result<ChatbotReplyData, ChatbotError> execute(ChatbotCommand command) {
        if (!rateLimitPort.tryAcquire(command.userId())) {
            return Result.fail(ChatbotError.RATE_LIMITED);
        }

        UUID classroomId = command.classroomId();
        ChatThreadData existingThread = null;
        List<ChatHistoryMessageData> history = List.of();

        if (command.threadId() != null) {
            Optional<ChatThreadData> thread = historyRepository.findThread(command.userId(), command.threadId());
            if (thread.isEmpty()) {
                return Result.fail(ChatbotError.THREAD_NOT_FOUND);
            }

            existingThread = thread.get();
            classroomId = existingThread.classroomId();
            history = selectModelHistory(historyRepository.findRecentMessages(
                    existingThread.threadId(),
                    MAX_HISTORY_MESSAGES_FOR_MODEL
            ));
        }

        String accessLevel;
        String verifiedContext;

        if (classroomId == null) {
            accessLevel = "DASHBOARD";
            verifiedContext = contextService.buildDashboardContext(command.userId());
        } else {
            ChatbotAccessService.ClassroomAccess access = accessService.getAccess(
                    command.userId(),
                    classroomId
            );

            if (access == ChatbotAccessService.ClassroomAccess.NONE) {
                return Result.fail(ChatbotError.ACCESS_DENIED);
            }

            accessLevel = access.name();
            verifiedContext = contextService.buildClassroomContext(
                    command.userId(),
                    classroomId,
                    access
            );
        }

        Result<String, ChatbotCompletionError> completion = completionPort.generateResponse(
                command.message(),
                accessLevel,
                verifiedContext,
                history
        );

        if (!completion.success()) {
            return Result.fail(mapCompletionError(completion.error()));
        }

        String reply = normalizeAssistantReply(completion.data());
        if (reply.isBlank()) {
            return Result.fail(ChatbotError.PROVIDER_UNAVAILABLE);
        }
        Optional<ChatThreadData> savedThread;

        if (existingThread == null) {
            savedThread = historyRepository.createThreadWithExchange(
                    command.userId(),
                    classroomId,
                    buildThreadTitle(command.message()),
                    command.message(),
                    reply
            );
        } else {
            savedThread = historyRepository.appendExchange(
                    command.userId(),
                    existingThread.threadId(),
                    command.message(),
                    reply
            );
        }

        if (savedThread.isEmpty()) {
            return Result.fail(ChatbotError.HISTORY_SAVE_FAILED);
        }

        ChatThreadData thread = savedThread.get();
        return Result.ok(new ChatbotReplyData(
                reply,
                thread.threadId(),
                thread.title(),
                thread.classroomId()
        ));
    }

    private String buildThreadTitle(String message) {
        String normalized = message.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= MAX_THREAD_TITLE_CHARACTERS) {
            return normalized;
        }
        return normalized.substring(0, MAX_THREAD_TITLE_CHARACTERS - 1).trim() + "…";
    }

    private String normalizeAssistantReply(String reply) {
        String normalized = reply == null ? "" : reply.trim();
        return normalized.length() <= MAX_ASSISTANT_REPLY_CHARACTERS
                ? normalized
                : normalized.substring(0, MAX_ASSISTANT_REPLY_CHARACTERS);
    }

    private List<ChatHistoryMessageData> selectModelHistory(List<ChatHistoryMessageData> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }

        int characterCount = 0;
        int startIndex = messages.size();
        for (int index = messages.size() - 1; index >= 0; index--) {
            String content = messages.get(index).content();
            int messageLength = content == null ? 0 : content.length();
            if (startIndex < messages.size()
                    && characterCount + messageLength > MAX_HISTORY_CHARACTERS_FOR_MODEL) {
                break;
            }
            characterCount += messageLength;
            startIndex = index;
        }

        if (startIndex < messages.size()
                && messages.get(startIndex).role() == ChatMessageRole.ASSISTANT) {
            startIndex += 1;
        }
        return List.copyOf(messages.subList(startIndex, messages.size()));
    }

    private ChatbotError mapCompletionError(ChatbotCompletionError error) {
        if (error == ChatbotCompletionError.NOT_CONFIGURED) {
            return ChatbotError.PROVIDER_NOT_CONFIGURED;
        }
        return ChatbotError.PROVIDER_UNAVAILABLE;
    }
}
