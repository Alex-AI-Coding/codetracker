package com.io.kira.infrastructure.user.persistence.repository;

import com.io.kira.infrastructure.user.persistence.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<UserEntity, UUID> {
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserEntity u SET u.profileUrl = :profileUrl WHERE u.userId = :userId")
    int updateProfileUrlByUserId(UUID userId, String profileUrl);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UserEntity u WHERE u.userId = :userId")
    Optional<UserEntity> findByIdForUpdate(@Param("userId") UUID userId);
}
