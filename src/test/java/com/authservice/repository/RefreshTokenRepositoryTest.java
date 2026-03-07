package com.authservice.repository;

import com.authservice.entity.RefreshToken;
import com.authservice.entity.Role;
import com.authservice.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("RefreshTokenRepository – Integration Tests (@DataJpaTest)")
class RefreshTokenRepositoryTest {

    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    private User user;

    @BeforeEach
    void setUp() {
        Role role = roleRepository.save(Role.builder().name("ROLE_USER").build());
        user = userRepository.save(User.builder()
                .username("testuser").email("test@example.com")
                .password("hashed").enabled(true).roles(Set.of(role)).build());
    }

    private RefreshToken save(String token, boolean revoked, Instant expiresAt) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .token(token).user(user).revoked(revoked).expiresAt(expiresAt).build());
    }

    @Test
    @DisplayName("findByToken – returns token when present")
    void findByToken_found() {
        save("tok-abc", false, Instant.now().plusSeconds(3600));

        Optional<RefreshToken> result = refreshTokenRepository.findByToken("tok-abc");

        assertThat(result).isPresent();
        assertThat(result.get().isRevoked()).isFalse();
    }

    @Test
    @DisplayName("revokeAllByUserId – marks all user tokens as revoked")
    void revokeAllByUserId() {
        save("tok-1", false, Instant.now().plusSeconds(3600));
        save("tok-2", false, Instant.now().plusSeconds(3600));

        refreshTokenRepository.revokeAllByUserId(user.getId());
        refreshTokenRepository.flush();

        List<RefreshToken> tokens = refreshTokenRepository.findAll();
        assertThat(tokens).allMatch(RefreshToken::isRevoked);
    }

    @Test
    @DisplayName("deleteExpiredAndRevoked – removes expired and revoked tokens")
    void deleteExpiredAndRevoked() {
        save("tok-expired", false, Instant.now().minusSeconds(1)); // expired
        save("tok-revoked", true,  Instant.now().plusSeconds(3600)); // revoked
        save("tok-valid",   false, Instant.now().plusSeconds(3600)); // keep this one

        refreshTokenRepository.deleteExpiredAndRevoked(Instant.now());
        refreshTokenRepository.flush();

        List<RefreshToken> remaining = refreshTokenRepository.findAll();
        assertThat(remaining).hasSize(1);
        assertThat(remaining.get(0).getToken()).isEqualTo("tok-valid");
    }
}
