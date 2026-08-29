package com.io.kira.adapter.user.out.persistence.repository;

import com.io.kira.application.user.port.out.UserPreferenceAppRepository;
import com.io.kira.domain.user.valueobject.ThemePreference;
import com.io.kira.infrastructure.user.persistence.entity.UserEntity;
import com.io.kira.infrastructure.user.persistence.entity.UserPreferenceEntity;
import com.io.kira.infrastructure.user.persistence.repository.JpaUserPreferenceRepository;
import com.io.kira.infrastructure.user.persistence.repository.JpaUserRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@AllArgsConstructor
public class UserPreferenceAppRepositoryImpl implements UserPreferenceAppRepository {

    private final JpaUserPreferenceRepository preferenceRepository;
    private final JpaUserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public Optional<ThemePreference> findThemePreference(UUID userId) {
        return preferenceRepository.findById(userId)
                .map(UserPreferenceEntity::getThemePreference);
    }

    @Override
    @Transactional
    public boolean saveThemePreference(UUID userId, ThemePreference preference) {
        // Serialize first-time preference creation across browsers/devices.
        Optional<UserEntity> user = userRepository.findByIdForUpdate(userId);
        if (user.isEmpty()) {
            return false;
        }

        UserPreferenceEntity entity = preferenceRepository.findById(userId)
                .orElseGet(() -> UserPreferenceEntity.builder()
                        .userEntity(user.get())
                        .themePreference(preference)
                        .build());

        entity.setUserEntity(user.get());
        entity.setThemePreference(preference);
        preferenceRepository.save(entity);
        return true;
    }
}
