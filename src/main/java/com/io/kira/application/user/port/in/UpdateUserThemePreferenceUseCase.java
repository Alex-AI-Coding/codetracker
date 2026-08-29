package com.io.kira.application.user.port.in;

import com.io.kira.domain.user.valueobject.ThemePreference;

import java.util.UUID;

public interface UpdateUserThemePreferenceUseCase {
    boolean updateThemePreference(UUID userId, ThemePreference preference);
}
