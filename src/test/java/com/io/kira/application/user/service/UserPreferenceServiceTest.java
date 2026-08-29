package com.io.kira.application.user.service;

import com.io.kira.application.user.port.out.UserPreferenceAppRepository;
import com.io.kira.domain.user.valueobject.ThemePreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserPreferenceServiceTest {

    private UserPreferenceAppRepository repository;
    private UserPreferenceService service;

    @BeforeEach
    void setUp() {
        repository = mock(UserPreferenceAppRepository.class);
        service = new UserPreferenceService(repository);
    }

    @Test
    void loadsSavedPreference() {
        UUID userId = UUID.randomUUID();
        when(repository.findThemePreference(userId)).thenReturn(Optional.of(ThemePreference.DARK));

        assertEquals(ThemePreference.DARK, service.getThemePreference(userId).orElseThrow());
    }

    @Test
    void savesValidPreference() {
        UUID userId = UUID.randomUUID();
        when(repository.saveThemePreference(userId, ThemePreference.LIGHT)).thenReturn(true);

        assertTrue(service.updateThemePreference(userId, ThemePreference.LIGHT));
        verify(repository).saveThemePreference(userId, ThemePreference.LIGHT);
    }

    @Test
    void rejectsMissingIdentityOrPreference() {
        UUID userId = UUID.randomUUID();

        assertFalse(service.updateThemePreference(null, ThemePreference.SYSTEM));
        assertFalse(service.updateThemePreference(userId, null));
        verify(repository, never()).saveThemePreference(userId, null);
    }
}
