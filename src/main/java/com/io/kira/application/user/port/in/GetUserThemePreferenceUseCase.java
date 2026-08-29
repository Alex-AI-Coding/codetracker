package com.io.kira.application.user.port.in;

import com.io.kira.domain.user.valueobject.ThemePreference;

import java.util.Optional;
import java.util.UUID;

public interface GetUserThemePreferenceUseCase {
    Optional<ThemePreference> getThemePreference(UUID userId);
}
