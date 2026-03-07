package com.authservice.service;

import com.authservice.entity.TokenBlacklist;
import com.authservice.entity.User;
import com.authservice.entity.Role;
import com.authservice.repository.TokenBlacklistRepository;
import com.authservice.util.TestRsaKeys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtService – Unit Tests")
class JwtServiceTest {

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    private JwtService jwtService;

    @BeforeEach
    void setUp() throws Exception {
        jwtService = new JwtService(tokenBlacklistRepository);
        ReflectionTestUtils.setField(jwtService, "privateKeyPem", TestRsaKeys.privateKeyPem());
        ReflectionTestUtils.setField(jwtService, "publicKeyPem",  TestRsaKeys.publicKeyPem());
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900L);
        ReflectionTestUtils.setField(jwtService, "issuer", "test-issuer");
        jwtService.initKeys();
    }

    // ---- Helpers ----

    private User buildUser() {
        Role role = Role.builder().id(UUID.randomUUID()).name("ROLE_USER").build();
        return User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email("test@example.com")
                .password("hashed")
                .enabled(true)
                .roles(Set.of(role))
                .build();
    }

    // ---- Tests ----

    @Test
    @DisplayName("generateAccessToken returns a non-blank JWT")
    void generateAccessToken_returnsToken() {
        String token = jwtService.generateAccessToken(buildUser());
        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    @DisplayName("validateAndParseClaims returns correct subject")
    void validateAndParseClaims_correctSubject() {
        User user = buildUser();
        String token = jwtService.generateAccessToken(user);

        String subject = jwtService.getSubject(token);

        assertThat(subject).isEqualTo(user.getId().toString());
    }

    @Test
    @DisplayName("isTokenValid returns true for a freshly issued, non-blacklisted token")
    void isTokenValid_validToken_returnsTrue() {
        given(tokenBlacklistRepository.existsByJti(any())).willReturn(false);
        String token = jwtService.generateAccessToken(buildUser());

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false for a blacklisted token")
    void isTokenValid_blacklistedToken_returnsFalse() {
        given(tokenBlacklistRepository.existsByJti(any())).willReturn(true);
        String token = jwtService.generateAccessToken(buildUser());

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid returns false for a tampered token")
    void isTokenValid_tamperedToken_returnsFalse() {
        String token = jwtService.generateAccessToken(buildUser());
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    @DisplayName("blacklistToken saves the JTI to the repository")
    void blacklistToken_savesJti() {
        String token = jwtService.generateAccessToken(buildUser());
        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);

        jwtService.blacklistToken(token);

        verify(tokenBlacklistRepository).save(captor.capture());
        assertThat(captor.getValue().getJti()).isNotBlank();
    }

    @Test
    @DisplayName("blacklistToken on an invalid token does not throw and does not save")
    void blacklistToken_invalidToken_noException() {
        assertThatCode(() -> jwtService.blacklistToken("not.a.jwt"))
                .doesNotThrowAnyException();
        verify(tokenBlacklistRepository, never()).save(any());
    }

    @Test
    @DisplayName("getExpiration returns a future instant for a fresh token")
    void getExpiration_futureInstant() {
        String token = jwtService.generateAccessToken(buildUser());

        assertThat(jwtService.getExpiration(token))
                .isAfter(java.time.Instant.now());
    }
}
