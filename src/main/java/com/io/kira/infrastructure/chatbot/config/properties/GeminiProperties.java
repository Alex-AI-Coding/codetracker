package com.io.kira.infrastructure.chatbot.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "chatbot.gemini")
public record GeminiProperties(
        String apiKey,
        String model,
        String baseUrl,
        int connectTimeoutMs,
        int readTimeoutMs
) {
    private static final String DEFAULT_MODEL = "gemini-3.5-flash";
    private static final String DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com";

    public GeminiProperties {
        apiKey = normalize(apiKey);
        model = hasText(model) ? model.trim() : DEFAULT_MODEL;
        baseUrl = hasText(baseUrl)
                ? removeTrailingSlash(baseUrl.trim())
                : DEFAULT_BASE_URL;
        connectTimeoutMs = connectTimeoutMs > 0 ? connectTimeoutMs : 5_000;
        readTimeoutMs = readTimeoutMs > 0 ? readTimeoutMs : 30_000;
    }

    public boolean isConfigured() {
        return hasText(apiKey);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String removeTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
