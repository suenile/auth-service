package com.authservice.service;

import com.authservice.dto.*;
import com.authservice.entity.RefreshToken;
import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.exception.AccountLockedException;
import com.authservice.exception.AuthException;
import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService – Unit Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock RoleRepository roleRepository;
    @Mock RefreshTokenRepository refreshTokenRepository;
    @Mock JwtService jwtService;
    @Mock MfaService mfaService;
    @Mock EmailService emailService;
    @Mock AuditLogService auditLogService;
    @Mock PasswordEncoder passwordEncoder;

    @InjectMocks
    AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpiration", 604800L);
        ReflectionTestUtils.setField(authService, "maxAttempts", 5);
        ReflectionTestUtils.setField(authService, "lockoutDurationMinutes", 15);
    }

    // ---- Helpers ----

    private Role userRole() {
        return Role.builder().id(UUID.randomUUID()).name("ROLE_USER").build();
    }

    private User enabledUser(String email, String encodedPassword) {
        return User.builder()
                .id(UUID.randomUUID())
                .username("testuser")
                .email(email)
                .password(encodedPassword)
                .enabled(true)
                .locked(false)
                .failedAttempts(0)
                .roles(new HashSet<>(Set.of(userRole())))
                .build();
    }

    private RegisterRequest registerRequest() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("testuser");
        req.setEmail("test@example.com");
        req.setPassword("Password1!");
        return req;
    }

    // ==================== REGISTER ====================

    @Nested
    @DisplayName("register()")
    class Register {

        @Test
        @DisplayName("happy path – saves user and sends verification email")
        void register_success() {
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(userRepository.existsByUsername(anyString())).willReturn(false);
            given(roleRepository.findByName("ROLE_USER")).willReturn(Optional.of(userRole()));
            given(userRepository.save(any(User.class))).willAnswer(inv -> {
                User u = inv.getArgument(0);
                ReflectionTestUtils.setField(u, "id", UUID.randomUUID());
                ReflectionTestUtils.setField(u, "createdAt", Instant.now());
                return u;
            });

            UserResponse res = authService.register(registerRequest());

            assertThat(res.getEmail()).isEqualTo("test@example.com");
            verify(emailService).sendVerificationEmail(eq("test@example.com"), anyString(), anyString());
        }

        @Test
        @DisplayName("throws AuthException when email already exists")
        void register_duplicateEmail_throws() {
            given(userRepository.existsByEmail("test@example.com")).willReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest()))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Email already in use");
        }

        @Test
        @DisplayName("throws AuthException when username already exists")
        void register_duplicateUsername_throws() {
            given(userRepository.existsByEmail(anyString())).willReturn(false);
            given(userRepository.existsByUsername("testuser")).willReturn(true);

            assertThatThrownBy(() -> authService.register(registerRequest()))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Username already taken");
        }
    }

    // ==================== LOGIN ====================

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("happy path – returns tokens on valid credentials")
        void login_success() {
            User user = enabledUser("test@example.com", "hashed");
            given(userRepository.findByEmail("test@example.com")).willReturn(Optional.of(user));
            given(passwordEncoder.matches("Password1!", "hashed")).willReturn(true);
            given(jwtService.generateAccessToken(user)).willReturn("access-token");
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            LoginRequest req = new LoginRequest();
            req.setUsernameOrEmail("test@example.com");
            req.setPassword("Password1!");

            TokenResponse res = authService.login(req, "127.0.0.1", "Test/1.0");

            assertThat(res.getAccessToken()).isEqualTo("access-token");
            assertThat(res.isMfaRequired()).isFalse();
        }

        @Test
        @DisplayName("throws AuthException for unknown user")
        void login_unknownUser_throws() {
            given(userRepository.findByEmail(anyString())).willReturn(Optional.empty());
            given(userRepository.findByUsername(anyString())).willReturn(Optional.empty());

            LoginRequest req = new LoginRequest();
            req.setUsernameOrEmail("nobody@example.com");
            req.setPassword("x");

            assertThatThrownBy(() -> authService.login(req, null, null))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Invalid credentials");
        }

        @Test
        @DisplayName("throws AuthException when account is not verified")
        void login_notEnabled_throws() {
            User user = User.builder()
                    .id(UUID.randomUUID()).username("u").email("test@example.com")
                    .password("hashed").enabled(false).locked(false).failedAttempts(0)
                    .roles(new HashSet<>(Set.of(userRole()))).build();
            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

            LoginRequest req = new LoginRequest();
            req.setUsernameOrEmail("test@example.com");
            req.setPassword("Password1!");

            assertThatThrownBy(() -> authService.login(req, null, null))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("not yet verified");
        }

        @Test
        @DisplayName("throws AccountLockedException when account is locked")
        void login_accountLocked_throws() {
            User user = User.builder()
                    .id(UUID.randomUUID()).username("u").email("test@example.com")
                    .password("hashed").enabled(true)
                    .locked(true).lockedUntil(Instant.now().plusSeconds(600))
                    .failedAttempts(5).roles(new HashSet<>(Set.of(userRole()))).build();
            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));

            LoginRequest req = new LoginRequest();
            req.setUsernameOrEmail("test@example.com");
            req.setPassword("Password1!");

            assertThatThrownBy(() -> authService.login(req, null, null))
                    .isInstanceOf(AccountLockedException.class);
        }

        @Test
        @DisplayName("throws AuthException on wrong password and increments failed attempts")
        void login_wrongPassword_incrementsFailedAttempts() {
            User user = enabledUser("test@example.com", "hashed");
            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(false);

            LoginRequest req = new LoginRequest();
            req.setUsernameOrEmail("test@example.com");
            req.setPassword("wrong");

            assertThatThrownBy(() -> authService.login(req, null, null))
                    .isInstanceOf(AuthException.class);
            verify(userRepository).incrementFailedAttempts(user.getId());
        }

        @Test
        @DisplayName("returns mfaRequired=true when MFA is enabled and no code provided")
        void login_mfaRequired() {
            User user = User.builder()
                    .id(UUID.randomUUID()).username("u").email("test@example.com")
                    .password("hashed").enabled(true).locked(false).failedAttempts(0)
                    .mfaEnabled(true).mfaSecret("SECRET")
                    .roles(new HashSet<>(Set.of(userRole()))).build();
            given(userRepository.findByEmail(anyString())).willReturn(Optional.of(user));
            given(passwordEncoder.matches(anyString(), anyString())).willReturn(true);

            LoginRequest req = new LoginRequest();
            req.setUsernameOrEmail("test@example.com");
            req.setPassword("Password1!");
            // mfaCode intentionally null

            TokenResponse res = authService.login(req, null, null);
            assertThat(res.isMfaRequired()).isTrue();
            assertThat(res.getAccessToken()).isNull();
        }
    }

    // ==================== REFRESH ====================

    @Nested
    @DisplayName("refreshTokens()")
    class RefreshTokens {

        @Test
        @DisplayName("returns new tokens and rotates the refresh token")
        void refresh_success() {
            User user = enabledUser("test@example.com", "hashed");
            RefreshToken stored = RefreshToken.builder()
                    .id(UUID.randomUUID())
                    .token("old-token")
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(604800))
                    .revoked(false)
                    .build();
            given(refreshTokenRepository.findByToken("old-token")).willReturn(Optional.of(stored));
            given(refreshTokenRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(jwtService.generateAccessToken(user)).willReturn("new-access");

            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("old-token");

            TokenResponse res = authService.refreshTokens(req);

            assertThat(res.getAccessToken()).isEqualTo("new-access");
            assertThat(stored.isRevoked()).isTrue(); // old token revoked
        }

        @Test
        @DisplayName("throws AuthException for a revoked token and revokes all user tokens")
        void refresh_revokedToken_throwsAndRevokesAll() {
            User user = enabledUser("test@example.com", "hashed");
            RefreshToken revoked = RefreshToken.builder()
                    .id(UUID.randomUUID()).token("revoked-token")
                    .user(user).expiresAt(Instant.now().plusSeconds(604800))
                    .revoked(true).build();
            given(refreshTokenRepository.findByToken("revoked-token")).willReturn(Optional.of(revoked));

            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("revoked-token");

            assertThatThrownBy(() -> authService.refreshTokens(req))
                    .isInstanceOf(AuthException.class);
            verify(refreshTokenRepository).revokeAllByUserId(user.getId());
        }

        @Test
        @DisplayName("throws AuthException for an expired token")
        void refresh_expiredToken_throws() {
            User user = enabledUser("test@example.com", "hashed");
            RefreshToken expired = RefreshToken.builder()
                    .id(UUID.randomUUID()).token("expired-token")
                    .user(user).expiresAt(Instant.now().minusSeconds(1))
                    .revoked(false).build();
            given(refreshTokenRepository.findByToken("expired-token")).willReturn(Optional.of(expired));

            RefreshTokenRequest req = new RefreshTokenRequest();
            req.setRefreshToken("expired-token");

            assertThatThrownBy(() -> authService.refreshTokens(req))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("expired");
        }
    }

    // ==================== EMAIL VERIFICATION ====================

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmail {

        @Test
        @DisplayName("enables user and clears verification token")
        void verifyEmail_success() {
            User user = User.builder()
                    .id(UUID.randomUUID()).username("u").email("test@example.com")
                    .password("hashed").enabled(false).locked(false).failedAttempts(0)
                    .verificationToken("tok123")
                    .verificationTokenExpires(Instant.now().plusSeconds(3600))
                    .roles(new HashSet<>(Set.of(userRole()))).build();
            given(userRepository.findByVerificationToken("tok123")).willReturn(Optional.of(user));
            given(userRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            authService.verifyEmail("tok123");

            assertThat(user.isEnabled()).isTrue();
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.getVerificationToken()).isNull();
        }

        @Test
        @DisplayName("throws AuthException for expired verification token")
        void verifyEmail_expiredToken_throws() {
            User user = User.builder()
                    .id(UUID.randomUUID()).username("u").email("test@example.com")
                    .password("hashed").enabled(false).locked(false).failedAttempts(0)
                    .verificationToken("expired-tok")
                    .verificationTokenExpires(Instant.now().minusSeconds(1))
                    .roles(new HashSet<>(Set.of(userRole()))).build();
            given(userRepository.findByVerificationToken("expired-tok")).willReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail("expired-tok"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("expired");
        }
    }

    // ==================== LOGOUT ====================

    @Nested
    @DisplayName("logout()")
    class Logout {

        @Test
        @DisplayName("blacklists access token and revokes all refresh tokens")
        void logout_blacklistsAndRevokes() {
            User user = enabledUser("test@example.com", "hashed");
            given(userRepository.findById(user.getId())).willReturn(Optional.of(user));

            authService.logout("access-token", user.getId());

            verify(jwtService).blacklistToken("access-token");
            verify(refreshTokenRepository).revokeAllByUserId(user.getId());
        }
    }
}
