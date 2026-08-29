package com.io.kira.infrastructure.user.persistence.repository;

import com.io.kira.infrastructure.user.persistence.entity.UserPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaUserPreferenceRepository extends JpaRepository<UserPreferenceEntity, UUID> {
}
