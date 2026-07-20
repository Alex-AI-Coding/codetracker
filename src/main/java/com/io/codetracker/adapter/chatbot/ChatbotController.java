package com.io.codetracker.adapter.chatbot;

import com.io.codetracker.adapter.auth.out.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    private final GeminiService geminiService;
    private final ChatbotAccessService chatbotAccessService;
    private final ChatbotContextService chatbotContextService;

    public ChatbotController(
            GeminiService geminiService,
            ChatbotAccessService chatbotAccessService,
            ChatbotContextService chatbotContextService
    ) {
        this.geminiService = geminiService;
        this.chatbotAccessService = chatbotAccessService;
        this.chatbotContextService = chatbotContextService;
    }

    @PostMapping
    public ResponseEntity<ChatbotResponse> chat(
            @AuthenticationPrincipal AuthPrincipal principal,
            @RequestBody ChatbotRequest request
    ) {

        if (principal == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new ChatbotResponse(
                            "You must be logged in to use the CodeTracker Assistant."
                    ));
        }

        if (request.message() == null || request.message().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(new ChatbotResponse(
                            "Please enter a message."
                    ));
        }

        String userId = principal.getUserId();

        String accessLevel = "GENERAL";
        String verifiedContext = "";

        if (
                request.classroomId() != null &&
                !request.classroomId().isBlank()
        ) {

            ChatbotAccessService.ClassroomAccess access =
                    chatbotAccessService.getAccess(
                            userId,
                            request.classroomId()
                    );

            if (access == ChatbotAccessService.ClassroomAccess.NONE) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(new ChatbotResponse(
                                "You do not have permission to access information from this classroom."
                        ));
            }

            accessLevel = access.name();

            verifiedContext =
                    chatbotContextService.buildContext(
                            userId,
                            request.classroomId(),
                            access
                    );
        }

        String reply = geminiService.generateResponse(
                request.message(),
                accessLevel,
                verifiedContext
        );

        return ResponseEntity.ok(
                new ChatbotResponse(reply)
        );
    }
}