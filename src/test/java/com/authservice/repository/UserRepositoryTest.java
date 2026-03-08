package com.authservice.repository;

import com.authservice.entity.Role;
import com.authservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("UserRepository – Integration Tests (@DataJpaTest)")
class UserRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    private Role userRole;

    @BeforeEach
    void setUp() {
        userRole = roleRepository.save(Role.builder().name("ROLE_USER").build());
    }

    private User persistUser(String username, String email) {
        return userRepository.save(User.builder()
                .username(username)
                .email(email)
                .password("hashed")
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build());
    }

    @Test
    @DisplayName("findByEmail – returns user when email matches")
    void findByEmail_found() {
        persistUser("alice", "alice@example.com");

        Optional<User> result = userRepository.findByEmail("alice@example.com");

        assertThat(result).isPresent();
        assertThat(result.get().getUsername()).isEqualTo("alice");
    }

    @Test
    @DisplayName("findByEmail – returns empty when email not found")
    void findByEmail_notFound() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    @DisplayName("findByUsername – returns user when username matches")
    void findByUsername_found() {
        persistUser("bob", "bob@example.com");

        Optional<User> result = userRepository.findByUsername("bob");

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo("bob@example.com");
    }

    @Test
    @DisplayName("existsByEmail – returns true for existing email")
    void existsByEmail_true() {
        persistUser("carol", "carol@example.com");
        assertThat(userRepository.existsByEmail("carol@example.com")).isTrue();
    }

    @Test
    @DisplayName("existsByEmail – returns false for unknown email")
    void existsByEmail_false() {
        assertThat(userRepository.existsByEmail("unknown@example.com")).isFalse();
    }

    @Test
    @DisplayName("existsByUsername – returns true for existing username")
    void existsByUsername_true() {
        persistUser("dave", "dave@example.com");
        assertThat(userRepository.existsByUsername("dave")).isTrue();
    }

    @Test
    @DisplayName("incrementFailedAttempts – increments counter by 1")
    void incrementFailedAttempts() {
        User user = persistUser("eve", "eve@example.com");
        assertThat(user.getFailedAttempts()).isZero();

        userRepository.incrementFailedAttempts(user.getId());
        userRepository.flush();

        User refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refreshed.getFailedAttempts()).isEqualTo(1);
    }

    @Test
    @DisplayName("resetFailedAttempts – sets counter to 0 and unlocks account")
    void resetFailedAttempts() {
        User user = persistUser("frank", "frank@example.com");
        userRepository.incrementFailedAttempts(user.getId());
        userRepository.incrementFailedAttempts(user.getId());
        userRepository.flush();

        userRepository.resetFailedAttempts(user.getId());
        userRepository.flush();

        User refreshed = userRepository.findById(user.getId()).orElseThrow();
        assertThat(refreshed.getFailedAttempts()).isZero();
        assertThat(refreshed.isLocked()).isFalse();
    }

    @Test
    @DisplayName("findByVerificationToken – returns user when token matches")
    void findByVerificationToken_found() {
        User user = User.builder()
                .username("grace").email("grace@example.com").password("hashed")
                .enabled(false).verificationToken("abc-token")
                .roles(new HashSet<>(Set.of(userRole))).build();
        userRepository.save(user);

        assertThat(userRepository.findByVerificationToken("abc-token")).isPresent();
        assertThat(userRepository.findByVerificationToken("wrong-token")).isEmpty();
    }
}
