package com.authservice.controller;

import com.authservice.dto.*;
import com.authservice.entity.User;
import com.authservice.repository.UserRepository;
import com.authservice.service.AuthService;
import com.authservice.service.JwtService;
import com.authservice.service.MfaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final MfaService mfaService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return ResponseEntity.ok(authService.login(request, ip, userAgent));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshTokens(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal UserDetails principal,
            HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            UUID userId = UUID.fromString(principal.getUsername());
            authService.logout(token, userId);
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully. You can now log in."));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.getEmail());
        // Always return success to avoid email enumeration
        return ResponseEntity.ok(Map.of("message", "If that email is registered, a reset link has been sent."));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Password has been reset successfully."));
    }

    @PostMapping("/mfa/enable")
    public ResponseEntity<Map<String, String>> enableMfa(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        User user = userRepository.findById(userId)
                .orElseThrow();

        String secret = mfaService.generateSecret();
        user.setMfaSecret(secret);
        userRepository.save(user);

        String qrUrl = mfaService.generateQrCodeUrl(secret, user.getEmail(), "AuthService");
        return ResponseEntity.ok(Map.of(
                "secret", secret,
                "qrCodeUrl", qrUrl
        ));
    }

    @PostMapping("/mfa/confirm")
    public ResponseEntity<Map<String, String>> confirmMfa(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MfaVerifyRequest request) {
        UUID userId = UUID.fromString(principal.getUsername());
        User user = userRepository.findById(userId).orElseThrow();

        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid MFA code"));
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "MFA enabled successfully"));
    }

    @PostMapping("/mfa/disable")
    public ResponseEntity<Map<String, String>> disableMfa(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody MfaVerifyRequest request) {
        UUID userId = UUID.fromString(principal.getUsername());
        User user = userRepository.findById(userId).orElseThrow();

        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid MFA code"));
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "MFA disabled successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me(@AuthenticationPrincipal UserDetails principal) {
        UUID userId = UUID.fromString(principal.getUsername());
        User user = userRepository.findById(userId).orElseThrow();
        return ResponseEntity.ok(authService.mapToResponse(user));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("valid", false, "message", "No token provided"));
        }
        String token = auth.substring(7);
        boolean valid = jwtService.isTokenValid(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
