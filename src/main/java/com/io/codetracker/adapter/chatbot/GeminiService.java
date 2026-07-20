package com.io.codetracker.adapter.chatbot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class GeminiService {

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/"
                    + "gemini-3.5-flash:generateContent";

    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public GeminiService(
            @Value("${GEMINI_API_KEY}") String apiKey
    ) {
        this.apiKey = apiKey;
        this.restClient = RestClient.create();
        this.objectMapper = new ObjectMapper();
    }

    public String generateResponse(
        String userMessage,
        String accessLevel,
        String verifiedContext
) {

        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of(
                                "parts", List.of(
                                        Map.of(
                                                "text",
                                                """
                                                You are CodeTracker Assistant, a helpful AI assistant
                                                for students and professors using the CodeTracker website.

                                                Be friendly, clear, and concise.

                                                Never claim that you can access private student,
                                                professor, classroom, assignment, or submission data
                                                unless that information has been securely provided to
                                                you by the CodeTracker backend.

                                                The user's verified CodeTracker access level is:
""" + accessLevel + """

Follow these access rules strictly:

INSTRUCTOR:
- May receive information about students and their work only for classrooms they own.
- May receive class-level information such as assignment results and who passed or failed.

STUDENT:
- May receive information about their own work and classrooms they joined.
- Must never receive another student's private work, grades, submissions, or personal information.

GENERAL:
- May ask general questions about CodeTracker and how to use the website.
- Do not provide private classroom or user data.

Never assume the user has permission beyond the verified access level provided above.

Verified data provided securely by the CodeTracker backend:

""" + verifiedContext + """

Important:
- Answer classroom-specific questions only using the verified data above.
- Do not invent student names, scores, submissions, grades, or feedback.
- If the requested information is not present, clearly say that you do not have enough verified data.
- Students must never receive another student's private information.

User message:
""" + userMessage
                                        )
                                )
                        )
                )
        );

        try {

            String response = restClient.post()
                    .uri(GEMINI_URL)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response);

            return root
                    .path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("Sorry, I could not generate a response.");

        } catch (Exception exception) {

            System.err.println(
                    "Gemini API error: " + exception.getMessage()
            );

            return "Sorry, I'm having trouble responding right now. Please try again.";
        }
    }
}