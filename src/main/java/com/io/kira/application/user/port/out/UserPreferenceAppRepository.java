package com.io.kira.application.user.port.out;

import com.io.kira.domain.user.valueobject.ThemePreference;

import java.util.Optional;
import java.util.UUID;

public interface UserPreferenceAppRepository {
    Optional<ThemePreference> findThemePreference(UUID userId);

    boolean saveThemePreference(UUID userId, ThemePreference preference);
}
