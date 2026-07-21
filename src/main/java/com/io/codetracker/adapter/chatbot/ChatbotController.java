package com.io.codetracker.adapter.chatbot;

import com.io.codetracker.adapter.auth.out.security.AuthPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

        if (request == null
                || request.message() == null
                || request.message().isBlank()) {
            return ResponseEntity
                    .badRequest()
                    .body(new ChatbotResponse("Please enter a message."));
        }

        UUID userId = principal.getUserId();

        // Dashboard is the default context. It contains only classrooms that
        // the authenticated user owns or has actively joined.
        String accessLevel = "DASHBOARD";
        String verifiedContext = chatbotContextService
                .buildDashboardContext(userId);

        // Classroom-specific data is added only after backend authorization.
        if (request.classroomId() != null) {
            ChatbotAccessService.ClassroomAccess access =
                    chatbotAccessService.getAccess(
                            userId,
                            request.classroomId()
                    );

            if (access == ChatbotAccessService.ClassroomAccess.NONE) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(new ChatbotResponse(
                                "You do not have permission to access "
                                        + "information from this classroom."
                        ));
            }

            accessLevel = access.name();
            verifiedContext = chatbotContextService.buildContext(
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

        return ResponseEntity.ok(new ChatbotResponse(reply));
    }
}
