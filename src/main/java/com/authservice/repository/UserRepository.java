package com.authservice.repository;

import com.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByVerificationToken(String token);

    Optional<User> findByPasswordResetToken(String token);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = u.failedAttempts + 1 WHERE u.id = :id")
    void incrementFailedAttempts(UUID id);

    @Modifying
    @Query("UPDATE User u SET u.failedAttempts = 0, u.locked = false, u.lockedUntil = null WHERE u.id = :id")
    void resetFailedAttempts(UUID id);
}
