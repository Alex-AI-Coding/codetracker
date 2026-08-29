package com.io.kira.adapter.user.in.rest;

import com.io.kira.adapter.auth.out.security.AuthPrincipal;
import com.io.kira.adapter.user.in.dto.request.ThemePreferenceRequest;
import com.io.kira.adapter.user.in.dto.response.ThemePreferenceResponse;
import com.io.kira.application.user.port.in.GetUserThemePreferenceUseCase;
import com.io.kira.application.user.port.in.UpdateUserThemePreferenceUseCase;
import com.io.kira.domain.user.valueobject.ThemePreference;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@AllArgsConstructor
@RequestMapping("/api/users/preferences/theme")
public class UserPreferenceController {

    private final GetUserThemePreferenceUseCase getThemePreferenceUseCase;
    private final UpdateUserThemePreferenceUseCase updateThemePreferenceUseCase;

    @GetMapping
    public ResponseEntity<ThemePreferenceResponse> getThemePreference(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Optional<ThemePreference> preference = getThemePreferenceUseCase
                .getThemePreference(principal.getUserId());

        return ResponseEntity.ok(new ThemePreferenceResponse(preference.orElse(null)));
    }

    @PatchMapping
    public ResponseEntity<ThemePreferenceResponse> updateThemePreference(
            @AuthenticationPrincipal AuthPrincipal principal,
            @Valid @RequestBody ThemePreferenceRequest request
    ) {
        boolean updated = updateThemePreferenceUseCase.updateThemePreference(
                principal.getUserId(),
                request.themePreference()
        );

        return updated
                ? ResponseEntity.ok(new ThemePreferenceResponse(request.themePreference()))
                : ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
