package com.io.kira.application.chatbot.service;

import com.io.kira.application.chatbot.error.ChatThreadError;
import com.io.kira.application.chatbot.port.out.ChatHistoryAppRepository;
import com.io.kira.application.chatbot.result.ChatMessagePageData;
import com.io.kira.application.chatbot.result.ChatThreadData;
import com.io.kira.application.chatbot.result.ChatThreadPageData;
import com.io.kira.common.result.Result;
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
import static org.mockito.Mockito.when;

class ChatThreadServiceTest {

    private ChatHistoryAppRepository repository;
    private ChatThreadService service;

    @BeforeEach
    void setUp() {
        repository = mock(ChatHistoryAppRepository.class);
        service = new ChatThreadService(repository);
    }

    @Test
    void doesNotLoadMessagesUntilOwnershipIsVerified() {
        UUID userId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        when(repository.findThread(userId, threadId)).thenReturn(Optional.empty());

        Result<ChatMessagePageData, ChatThreadError> result =
                service.getMessages(userId, threadId, null, 100);

        assertFalse(result.success());
        assertEquals(ChatThreadError.THREAD_NOT_FOUND, result.error());
        verify(repository, never()).findMessagePage(threadId, null, 100);
    }

    @Test
    void clampsThreadPaginationToSafeBounds() {
        UUID userId = UUID.randomUUID();
        ChatThreadPageData emptyPage = new ChatThreadPageData(List.of(), 0, 100, false);
        when(repository.findThreads(userId, 0, 100)).thenReturn(emptyPage);

        ChatThreadPageData result = service.listThreads(userId, -5, 10_000);

        assertEquals(emptyPage, result);
        verify(repository).findThreads(userId, 0, 100);
    }

    @Test
    void clampsMessagePaginationAfterOwnershipCheck() {
        UUID userId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        Instant now = Instant.now();
        ChatThreadData owned = new ChatThreadData(threadId, null, "Owned", now, now);
        ChatMessagePageData page = new ChatMessagePageData(List.of(), null, false);
        when(repository.findThread(userId, threadId)).thenReturn(Optional.of(owned));
        when(repository.findMessagePage(threadId, null, 200)).thenReturn(page);

        Result<ChatMessagePageData, ChatThreadError> result =
                service.getMessages(userId, threadId, -1, 5_000);

        assertTrue(result.success());
        assertEquals(page, result.data());
        verify(repository).findMessagePage(threadId, null, 200);
    }

    @Test
    void normalizesTitleBeforeRenaming() {
        UUID userId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        Instant now = Instant.now();
        ChatThreadData renamed = new ChatThreadData(threadId, null, "My thread", now, now);
        when(repository.renameThread(userId, threadId, "My thread"))
                .thenReturn(Optional.of(renamed));

        Result<ChatThreadData, ChatThreadError> result =
                service.renameThread(userId, threadId, "  My   thread  ");

        assertTrue(result.success());
        assertEquals("My thread", result.data().title());
    }

    @Test
    void rejectsBlankTitleWithoutCallingRepository() {
        UUID userId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();

        Result<ChatThreadData, ChatThreadError> result =
                service.renameThread(userId, threadId, "   ");

        assertFalse(result.success());
        assertEquals(ChatThreadError.INVALID_TITLE, result.error());
        verify(repository, never()).renameThread(userId, threadId, "");
    }
}
