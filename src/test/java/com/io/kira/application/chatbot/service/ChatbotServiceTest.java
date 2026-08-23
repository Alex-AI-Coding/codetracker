package com.io.kira.application.chatbot.service;

import com.io.kira.application.chatbot.command.ChatbotCommand;
import com.io.kira.application.chatbot.error.ChatbotCompletionError;
import com.io.kira.application.chatbot.error.ChatbotError;
import com.io.kira.application.chatbot.port.out.ChatbotCompletionPort;
import com.io.kira.application.chatbot.result.ChatbotReplyData;
import com.io.kira.common.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatbotServiceTest {

    private ChatbotAccessService accessService;
    private ChatbotContextService contextService;
    private ChatbotCompletionPort completionPort;
    private ChatbotService service;

    @BeforeEach
    void setUp() {
        accessService = mock(ChatbotAccessService.class);
        contextService = mock(ChatbotContextService.class);
        completionPort = mock(ChatbotCompletionPort.class);
        service = new ChatbotService(accessService, contextService, completionPort);
    }

    @Test
    void usesOnlyDashboardContextWhenNoClassroomIsSelected() {
        UUID userId = UUID.randomUUID();
        when(contextService.buildDashboardContext(userId)).thenReturn("verified dashboard");
        when(completionPort.generateResponse("What classes do I have?", "DASHBOARD", "verified dashboard"))
                .thenReturn(Result.ok("You have two classes."));

        Result<ChatbotReplyData, ChatbotError> result = service.execute(
                new ChatbotCommand(userId, "What classes do I have?", null)
        );

        assertTrue(result.success());
        assertEquals("You have two classes.", result.data().reply());
        verify(accessService, never()).getAccess(userId, null);
    }

    @Test
    void stopsBeforeLoadingPrivateContextWhenAccessIsDenied() {
        UUID userId = UUID.randomUUID();
        UUID classroomId = UUID.randomUUID();
        when(accessService.getAccess(userId, classroomId))
                .thenReturn(ChatbotAccessService.ClassroomAccess.NONE);

        Result<ChatbotReplyData, ChatbotError> result = service.execute(
                new ChatbotCommand(userId, "Show the grades", classroomId)
        );

        assertFalse(result.success());
        assertEquals(ChatbotError.ACCESS_DENIED, result.error());
        verify(contextService, never()).buildClassroomContext(
                userId,
                classroomId,
                ChatbotAccessService.ClassroomAccess.NONE
        );
        verify(contextService, never()).buildDashboardContext(userId);
        verifyNoInteractions(completionPort);
    }

    @Test
    void reportsMissingGeminiConfigurationWithoutCrashingTheApplication() {
        UUID userId = UUID.randomUUID();
        when(contextService.buildDashboardContext(userId)).thenReturn("verified dashboard");
        when(completionPort.generateResponse("Hello", "DASHBOARD", "verified dashboard"))
                .thenReturn(Result.fail(ChatbotCompletionError.NOT_CONFIGURED));

        Result<ChatbotReplyData, ChatbotError> result = service.execute(
                new ChatbotCommand(userId, "Hello", null)
        );

        assertFalse(result.success());
        assertEquals(ChatbotError.PROVIDER_NOT_CONFIGURED, result.error());
    }
}
