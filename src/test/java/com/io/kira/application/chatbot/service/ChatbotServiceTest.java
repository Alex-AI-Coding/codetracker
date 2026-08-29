package com.io.kira.application.chatbot.service;

import com.io.kira.application.chatbot.command.ChatbotCommand;
import com.io.kira.application.chatbot.error.ChatbotCompletionError;
import com.io.kira.application.chatbot.error.ChatbotError;
import com.io.kira.application.chatbot.port.out.ChatHistoryAppRepository;
import com.io.kira.application.chatbot.port.out.ChatbotCompletionPort;
import com.io.kira.application.chatbot.port.out.ChatbotRateLimitPort;
import com.io.kira.application.chatbot.result.ChatHistoryMessageData;
import com.io.kira.application.chatbot.result.ChatThreadData;
import com.io.kira.application.chatbot.result.ChatbotReplyData;
import com.io.kira.common.result.Result;
import com.io.kira.domain.chatbot.valueobject.ChatMessageRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ChatbotServiceTest {

    private ChatbotAccessService accessService;
    private ChatbotContextService contextService;
    private ChatbotCompletionPort completionPort;
    private ChatHistoryAppRepository historyRepository;
    private ChatbotRateLimitPort rateLimitPort;
    private ChatbotService service;

    @BeforeEach
    void setUp() {
        accessService = mock(ChatbotAccessService.class);
        contextService = mock(ChatbotContextService.class);
        completionPort = mock(ChatbotCompletionPort.class);
        historyRepository = mock(ChatHistoryAppRepository.class);
        rateLimitPort = mock(ChatbotRateLimitPort.class);
        when(rateLimitPort.tryAcquire(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        service = new ChatbotService(
                accessService,
                contextService,
                completionPort,
                historyRepository,
                rateLimitPort
        );
    }

    @Test
    void rejectsRateLimitedRequestBeforeLoadingContextOrCallingGemini() {
        UUID userId = UUID.randomUUID();
        when(rateLimitPort.tryAcquire(userId)).thenReturn(false);

        Result<ChatbotReplyData, ChatbotError> result = service.execute(
                new ChatbotCommand(userId, "Hello", null, null)
        );

        assertFalse(result.success());
        assertEquals(ChatbotError.RATE_LIMITED, result.error());
        verifyNoInteractions(accessService, contextService, completionPort, historyRepository);
    }

    @Test
    void createsSavedDashboardThreadForFirstExchange() {
        UUID userId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        ChatThreadData saved = thread(threadId, null, "What classes do I have?");

        when(contextService.buildDashboardContext(userId)).thenReturn("verified dashboard");
        when(completionPort.generateResponse(
                "What classes do I have?", "DASHBOARD", "verified dashboard", List.of()
        )).thenReturn(Result.ok("You have two classes."));
        when(historyRepository.createThreadWithExchange(
                userId, null, "What classes do I have?", "What classes do I have?", "You have two classes."
        )).thenReturn(Optional.of(saved));

        Result<ChatbotReplyData, ChatbotError> result = service.execute(
                new ChatbotCommand(userId, "What classes do I have?", null, null)
        );

        assertTrue(result.success());
        assertEquals("You have two classes.", result.data().reply());
        assertEquals(threadId, result.data().threadId());
        verify(accessService, never()).getAccess(userId, null);
    }

    @Test
    void continuesOwnedThreadUsingStoredClassroomAndHistory() {
        UUID userId = UUID.randomUUID();
        UUID storedClassroomId = UUID.randomUUID();
        UUID untrustedRequestClassroomId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        ChatThreadData existing = thread(threadId, storedClassroomId, "Grades");
        List<ChatHistoryMessageData> history = List.of(
                new ChatHistoryMessageData(
                        UUID.randomUUID(), ChatMessageRole.USER, "What is my score?", 1, Instant.now()
                )
        );

        when(historyRepository.findThread(userId, threadId)).thenReturn(Optional.of(existing));
        when(historyRepository.findRecentMessages(threadId, 20)).thenReturn(history);
        when(accessService.getAccess(userId, storedClassroomId))
                .thenReturn(ChatbotAccessService.ClassroomAccess.STUDENT);
        when(contextService.buildClassroomContext(
                userId, storedClassroomId, ChatbotAccessService.ClassroomAccess.STUDENT
        )).thenReturn("verified classroom");
        when(completionPort.generateResponse(
                "And the feedback?", "STUDENT", "verified classroom", history
        )).thenReturn(Result.ok("Your feedback is clear."));
        when(historyRepository.appendExchange(
                userId, threadId, "And the feedback?", "Your feedback is clear."
        )).thenReturn(Optional.of(existing));

        Result<ChatbotReplyData, ChatbotError> result = service.execute(
                new ChatbotCommand(userId, "And the feedback?", untrustedRequestClassroomId, threadId)
        );

        assertTrue(result.success());
        assertEquals(storedClassroomId, result.data().classroomId());
        verify(accessService).getAccess(userId, storedClassroomId);
        verify(accessService, never()).getAccess(userId, untrustedRequestClassroomId);
    }

    @Test
    void stopsBeforeLoadingPrivateContextWhenAccessIsDenied() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        when(accessService.getAccess(userId, classroomId))
                .thenReturn(ChatbotAccessService.ClassroomAccess.NONE);

        Result<ChatbotReplyData, ChatbotError> result = service.execute(
                new ChatbotCommand(userId, "Show the grades", classroomId, null)
        );

        assertFalse(result.success());
        assertEquals(ChatbotError.ACCESS_DENIED, result.error());
        verify(contextService, never()).buildClassroomContext(
                userId, classroomId, ChatbotAccessService.ClassroomAccess.NONE
        );
        verify(contextService, never()).buildDashboardContext(userId);
        verifyNoInteractions(completionPort);
    }

    @Test
    void rejectsThreadThatDoesNotBelongToUser() {
        UUID userId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        when(historyRepository.findThread(userId, threadId)).thenReturn(Optional.empty());

        Result<ChatbotReplyData, ChatbotError> result = service.execute(
                new ChatbotCommand(userId, "Continue", null, threadId)
        );

        assertFalse(result.success());
        assertEquals(ChatbotError.THREAD_NOT_FOUND, result.error());
        verifyNoInteractions(accessService, contextService, completionPort);
    }

    @Test
    void reportsMissingGeminiConfigurationWithoutSaving() {
        UUID userId = UUID.randomUUID();
        when(contextService.buildDashboardContext(userId)).thenReturn("verified dashboard");
        when(completionPort.generateResponse("Hello", "DASHBOARD", "verified dashboard", List.of()))
                .thenReturn(Result.fail(ChatbotCompletionError.NOT_CONFIGURED));

        Result<ChatbotReplyData, ChatbotError> result = service.execute(
                new ChatbotCommand(userId, "Hello", null, null)
        );

        assertFalse(result.success());
        assertEquals(ChatbotError.PROVIDER_NOT_CONFIGURED, result.error());
        verifyNoInteractions(historyRepository);
    }

    @Test
    void failsSafelyWhenGeneratedExchangeCannotBeSaved() {
        UUID userId = UUID.randomUUID();
        when(contextService.buildDashboardContext(userId)).thenReturn("verified dashboard");
        when(completionPort.generateResponse("Hello", "DASHBOARD", "verified dashboard", List.of()))
                .thenReturn(Result.ok("Hi there."));
        when(historyRepository.createThreadWithExchange(
                userId, null, "Hello", "Hello", "Hi there."
        )).thenReturn(Optional.empty());

        Result<ChatbotReplyData, ChatbotError> result = service.execute(
                new ChatbotCommand(userId, "Hello", null, null)
        );

        assertFalse(result.success());
        assertEquals(ChatbotError.HISTORY_SAVE_FAILED, result.error());
    }

    private ChatThreadData thread(UUID threadId, UUID classroomId, String title) {
        Instant now = Instant.now();
        return new ChatThreadData(threadId, classroomId, title, now, now);
    }
}
