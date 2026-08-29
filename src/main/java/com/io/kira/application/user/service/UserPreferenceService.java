package com.io.kira.application.user.service;

import com.io.kira.application.user.port.in.GetUserThemePreferenceUseCase;
import com.io.kira.application.user.port.in.UpdateUserThemePreferenceUseCase;
import com.io.kira.application.user.port.out.UserPreferenceAppRepository;
import com.io.kira.domain.user.valueobject.ThemePreference;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public final class UserPreferenceService implements GetUserThemePreferenceUseCase, UpdateUserThemePreferenceUseCase {

    private final UserPreferenceAppRepository repository;

    @Override
    public Optional<ThemePreference> getThemePreference(UUID userId) {
        return repository.findThemePreference(userId);
    }

    @Override
    public boolean updateThemePreference(UUID userId, ThemePreference preference) {
        if (userId == null || preference == null) {
            return false;
        }
        return repository.saveThemePreference(userId, preference);
    }
}
