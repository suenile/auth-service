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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final MfaService mfaService;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository,
                       RefreshTokenRepository refreshTokenRepository, JwtService jwtService,
                       MfaService mfaService, EmailService emailService,
                       AuditLogService auditLogService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.mfaService = mfaService;
        this.emailService = emailService;
        this.auditLogService = auditLogService;
        this.passwordEncoder = passwordEncoder;
    }

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    @Value("${security.account-lockout.max-attempts}")
    private int maxAttempts;

    @Value("${security.account-lockout.lockout-duration-minutes}")
    private int lockoutDurationMinutes;

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AuthException("Email already in use");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AuthException("Username already taken");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new AuthException("Default role not found"));

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(false)
                .roles(new HashSet<>(Set.of(userRole)))
                .verificationToken(verificationToken)
                .verificationTokenExpires(Instant.now().plusSeconds(86400)) // 24 hours
                .build();

        user = userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), verificationToken, "https://localhost:8443");
        auditLogService.log(user, "USER_REGISTERED", null, null, "User registered: " + user.getEmail());

        return mapToResponse(user);
    }

    @Transactional
    public TokenResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getUsernameOrEmail())
                .or(() -> userRepository.findByUsername(request.getUsernameOrEmail()))
                .orElseThrow(() -> new AuthException("Invalid credentials"));

        // Check if account is locked
        if (user.isLocked()) {
            if (user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil())) {
                throw new AccountLockedException("Account is temporarily locked. Try again later.");
            } else {
                // Unlock if lockout period expired
                userRepository.resetFailedAttempts(user.getId());
                user.setLocked(false);
                user.setLockedUntil(null);
            }
        }

        if (!user.isEnabled()) {
            throw new AuthException("Account is not yet verified. Please check your email.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            handleFailedAttempt(user);
            auditLogService.log(user, "LOGIN_FAILED", ipAddress, userAgent, "Bad credentials");
            throw new AuthException("Invalid credentials");
        }

        // MFA check
        if (user.isMfaEnabled()) {
            if (request.getMfaCode() == null || request.getMfaCode().isBlank()) {
                return TokenResponse.builder()
                        .mfaRequired(true)
                        .tokenType("Bearer")
                        .build();
            }
            if (!mfaService.verifyCode(user.getMfaSecret(), request.getMfaCode())) {
                auditLogService.log(user, "MFA_FAILED", ipAddress, userAgent, "Invalid MFA code");
                throw new AuthException("Invalid MFA code");
            }
        }

        // Reset failed attempts on successful login
        userRepository.resetFailedAttempts(user.getId());

        String accessToken = jwtService.generateAccessToken(user);
        RefreshToken refreshToken = createRefreshToken(user);

        auditLogService.log(user, "LOGIN_SUCCESS", ipAddress, userAgent, null);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900)
                .mfaRequired(false)
                .build();
    }

    @Transactional
    public TokenResponse refreshTokens(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new AuthException("Invalid refresh token"));

        if (stored.isRevoked()) {
            // Possible token reuse attack — revoke all tokens for this user
            refreshTokenRepository.revokeAllByUserId(stored.getUser().getId());
            auditLogService.log(stored.getUser(), "REFRESH_TOKEN_REUSE_ATTACK", null, null, null);
            throw new AuthException("Refresh token has been revoked");
        }

        if (stored.isExpired()) {
            throw new AuthException("Refresh token has expired");
        }

        // Rotate: revoke old, issue new
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        User user = stored.getUser();
        String newAccessToken = jwtService.generateAccessToken(user);
        RefreshToken newRefreshToken = createRefreshToken(user);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(900)
                .mfaRequired(false)
                .build();
    }

    @Transactional
    public void logout(String accessToken, UUID userId) {
        jwtService.blacklistToken(accessToken);
        refreshTokenRepository.revokeAllByUserId(userId);
        userRepository.findById(userId).ifPresent(u ->
                auditLogService.log(u, "LOGOUT", null, null, null));
    }

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new AuthException("Invalid verification token"));

        if (user.getVerificationTokenExpires() != null
                && Instant.now().isAfter(user.getVerificationTokenExpires())) {
            throw new AuthException("Verification token has expired");
        }

        user.setEmailVerified(true);
        user.setEnabled(true);
        user.setVerificationToken(null);
        user.setVerificationTokenExpires(null);
        userRepository.save(user);
        auditLogService.log(user, "EMAIL_VERIFIED", null, null, null);
    }

    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setPasswordResetToken(token);
            user.setPasswordResetExpires(Instant.now().plusSeconds(3600)); // 1 hour
            userRepository.save(user);
            emailService.sendPasswordResetEmail(email, token, "https://localhost:8443");
            auditLogService.log(user, "PASSWORD_RESET_REQUESTED", null, null, null);
        });
    }

    @Transactional
    public void resetPassword(PasswordResetRequest request) {
        User user = userRepository.findByPasswordResetToken(request.getToken())
                .orElseThrow(() -> new AuthException("Invalid or expired reset token"));

        if (user.getPasswordResetExpires() != null
                && Instant.now().isAfter(user.getPasswordResetExpires())) {
            throw new AuthException("Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetExpires(null);
        userRepository.save(user);

        // Revoke all refresh tokens after password change
        refreshTokenRepository.revokeAllByUserId(user.getId());
        auditLogService.log(user, "PASSWORD_RESET_COMPLETED", null, null, null);
    }

    // ---- Private Helpers ----

    private RefreshToken createRefreshToken(User user) {
        RefreshToken token = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .expiresAt(Instant.now().plusSeconds(refreshTokenExpiration))
                .build();
        return refreshTokenRepository.save(token);
    }

    private void handleFailedAttempt(User user) {
        userRepository.incrementFailedAttempts(user.getId());
        int attempts = user.getFailedAttempts() + 1;
        if (attempts >= maxAttempts) {
            user.setLocked(true);
            user.setLockedUntil(Instant.now().plusSeconds(lockoutDurationMinutes * 60L));
            userRepository.save(user);
            log.warn("Account locked for user: {} after {} failed attempts", user.getEmail(), attempts);
        }
    }

    public UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .mfaEnabled(user.isMfaEnabled())
                .emailVerified(user.isEmailVerified())
                .roles(user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
