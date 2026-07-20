package com.io.codetracker.adapter.chatbot;

public record ChatbotRequest(
        String message,
        String classroomId
) {
}