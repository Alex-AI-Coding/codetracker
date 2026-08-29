package com.io.kira.adapter.chatbot.in.rest;

import com.io.kira.adapter.auth.out.security.AuthPrincipal;
import com.io.kira.adapter.chatbot.in.dto.request.RenameChatThreadRequest;
import com.io.kira.adapter.chatbot.in.dto.response.ChatMessagesResponse;
import com.io.kira.adapter.chatbot.in.dto.response.ChatThreadResponse;
import com.io.kira.adapter.chatbot.in.dto.response.ChatThreadsResponse;
import com.io.kira.application.chatbot.error.ChatThreadError;
import com.io.kira.application.chatbot.port.in.ChatThreadUseCase;
import com.io.kira.application.chatbot.result.ChatMessagePageData;
import com.io.kira.application.chatbot.result.ChatThreadData;
import com.io.kira.common.result.Result;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/api/chatbot/threads")
public class ChatThreadController {

    private final ChatThreadUseCase chatThreadUseCase;

    @GetMapping
    public ResponseEntity<ChatThreadsResponse> listThreads(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(ChatThreadsResponse.from(
                chatThreadUseCase.listThreads(principal.getUserId(), page, size)
        ));
    }

    @GetMapping("/{threadId}/messages")
    public ResponseEntity<ChatMessagesResponse> getMessages(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID threadId,
            @RequestParam(required = false) Integer beforePosition,
            @RequestParam(defaultValue = "100") int size
    ) {
        Result<ChatMessagePageData, ChatThreadError> result = chatThreadUseCase
                .getMessages(principal.getUserId(), threadId, beforePosition, size);

        return result.success()
                ? ResponseEntity.ok(ChatMessagesResponse.success(result.data()))
                : ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ChatMessagesResponse.fail("Conversation not found."));
    }

    @PatchMapping("/{threadId}")
    public ResponseEntity<ChatThreadResponse> renameThread(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID threadId,
            @Valid @RequestBody RenameChatThreadRequest request
    ) {
        Result<ChatThreadData, ChatThreadError> result = chatThreadUseCase.renameThread(
                principal.getUserId(),
                threadId,
                request.title()
        );

        if (result.success()) {
            return ResponseEntity.ok(ChatThreadResponse.success(result.data()));
        }

        HttpStatus status = result.error() == ChatThreadError.INVALID_TITLE
                ? HttpStatus.BAD_REQUEST
                : HttpStatus.NOT_FOUND;
        return ResponseEntity.status(status).body(ChatThreadResponse.fail(
                result.error() == ChatThreadError.INVALID_TITLE
                        ? "Thread title is invalid."
                        : "Conversation not found."
        ));
    }

    @DeleteMapping("/{threadId}")
    public ResponseEntity<Void> deleteThread(
            @AuthenticationPrincipal AuthPrincipal principal,
            @PathVariable UUID threadId
    ) {
        Result<Void, ChatThreadError> result = chatThreadUseCase.deleteThread(
                principal.getUserId(),
                threadId
        );

        return result.success()
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
