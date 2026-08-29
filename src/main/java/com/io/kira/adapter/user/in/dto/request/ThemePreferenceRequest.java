package com.io.kira.adapter.user.in.dto.request;

import com.io.kira.domain.user.valueobject.ThemePreference;
import jakarta.validation.constraints.NotNull;

public record ThemePreferenceRequest(
        @NotNull(message = "Theme preference is required")
        ThemePreference themePreference
) {
}
