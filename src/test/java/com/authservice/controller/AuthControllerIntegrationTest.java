package com.authservice.controller;

import com.authservice.dto.LoginRequest;
import com.authservice.dto.RegisterRequest;
import com.authservice.entity.Role;
import com.authservice.entity.User;
import com.authservice.repository.RoleRepository;
import com.authservice.repository.UserRepository;
import com.authservice.service.EmailService;
import com.authservice.util.TestRsaKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController – Integration Tests")
class AuthControllerIntegrationTest {

    @DynamicPropertySource
    static void overrideJwtKeys(DynamicPropertyRegistry registry) {
        registry.add("jwt.private-key", TestRsaKeys::privateKeyPem);
        registry.add("jwt.public-key",  TestRsaKeys::publicKeyPem);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRepository userRepository;

    /** Prevent actual emails being sent during tests. */
    @MockBean EmailService emailService;

    @BeforeEach
    void ensureRoleExists() {
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            roleRepository.save(Role.builder().name("ROLE_USER").build());
        }
    }

    // ---- Helpers ----

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private RegisterRequest validRegisterRequest(String username, String email) {
        RegisterRequest req = new RegisterRequest();
        req.setUsername(username);
        req.setEmail(email);
        req.setPassword("SecurePass1!");
        return req;
    }

    /**
     * Registers a user and immediately enables them (bypasses email verification),
     * simulating a fully verified account ready for login.
     */
    private void registerAndEnable(String username, String email) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(validRegisterRequest(username, email))))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail(email).orElseThrow();
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);
    }

    // ==================== REGISTER ====================

    @Nested
    @DisplayName("POST /api/auth/register")
    class Register {

        @Test
        @DisplayName("returns 201 with user details on valid request")
        void register_validRequest_returns201() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(validRegisterRequest("alice", "alice@example.com"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.email").value("alice@example.com"))
                    .andExpect(jsonPath("$.username").value("alice"))
                    .andExpect(jsonPath("$.enabled").value(false));
        }

        @Test
        @DisplayName("returns 400 when email is invalid")
        void register_invalidEmail_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("bob");
            req.setEmail("not-an-email");
            req.setPassword("SecurePass1!");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.email").exists());
        }

        @Test
        @DisplayName("returns 400 when password is too short")
        void register_shortPassword_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setUsername("carol");
            req.setEmail("carol@example.com");
            req.setPassword("short");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 401 when email is already registered")
        void register_duplicateEmail_returns401() throws Exception {
            RegisterRequest req = validRegisterRequest("dave", "dave@example.com");
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(req)));

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 400 when username is missing")
        void register_missingUsername_returns400() throws Exception {
            RegisterRequest req = new RegisterRequest();
            req.setEmail("eve@example.com");
            req.setPassword("SecurePass1!");

            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors.username").exists());
        }
    }

    // ==================== LOGIN ====================

    @Nested
    @DisplayName("POST /api/auth/login")
    class Login {

        @Test
        @DisplayName("returns 200 with tokens for a verified user with correct credentials")
        void login_verifiedUser_returnsTokens() throws Exception {
            registerAndEnable("grace", "grace@example.com");

            LoginRequest req = new LoginRequest();
            req.setUsernameOrEmail("grace@example.com");
            req.setPassword("SecurePass1!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.mfaRequired").value(false));
        }

        @Test
        @DisplayName("returns 401 for unknown user")
        void login_unknownUser_returns401() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsernameOrEmail("nobody@example.com");
            req.setPassword("Password1!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 401 for unverified account")
        void login_unverifiedAccount_returns401() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(toJson(validRegisterRequest("frank", "frank@example.com"))));

            LoginRequest req = new LoginRequest();
            req.setUsernameOrEmail("frank@example.com");
            req.setPassword("SecurePass1!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value(
                            org.hamcrest.Matchers.containsString("not yet verified")));
        }

        @Test
        @DisplayName("returns 401 for wrong password")
        void login_wrongPassword_returns401() throws Exception {
            registerAndEnable("henry", "henry@example.com");

            LoginRequest req = new LoginRequest();
            req.setUsernameOrEmail("henry@example.com");
            req.setPassword("WrongPass999!");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(req)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("returns 400 when request body is missing required fields")
        void login_missingFields_returns400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== TOKEN REFRESH ====================

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class TokenRefresh {

        @Test
        @DisplayName("returns new tokens for a valid refresh token")
        void refresh_validToken_returnsNewTokens() throws Exception {
            registerAndEnable("ivan", "ivan@example.com");

            LoginRequest loginReq = new LoginRequest();
            loginReq.setUsernameOrEmail("ivan@example.com");
            loginReq.setPassword("SecurePass1!");

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            Map<?, ?> loginBody = objectMapper.readValue(
                    loginResult.getResponse().getContentAsString(), Map.class);
            String refreshToken = (String) loginBody.get("refreshToken");

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").isNotEmpty())
                    .andExpect(jsonPath("$.refreshToken").isNotEmpty());
        }

        @Test
        @DisplayName("returns 401 for an invalid refresh token")
        void refresh_invalidToken_returns401() throws Exception {
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"totally-invalid-token\"}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== VALIDATE TOKEN ====================

    @Nested
    @DisplayName("GET /api/auth/validate")
    class ValidateToken {

        @Test
        @DisplayName("returns valid=true for a freshly issued access token")
        void validate_validToken_returnsTrue() throws Exception {
            registerAndEnable("judy", "judy@example.com");

            LoginRequest loginReq = new LoginRequest();
            loginReq.setUsernameOrEmail("judy@example.com");
            loginReq.setPassword("SecurePass1!");

            MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(toJson(loginReq)))
                    .andExpect(status().isOk())
                    .andReturn();

            Map<?, ?> loginBody = objectMapper.readValue(
                    loginResult.getResponse().getContentAsString(), Map.class);
            String accessToken = (String) loginBody.get("accessToken");

            mockMvc.perform(get("/api/auth/validate")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.valid").value(true));
        }

        @Test
        @DisplayName("returns 401 when no Authorization header present")
        void validate_noHeader_returns401() throws Exception {
            mockMvc.perform(get("/api/auth/validate"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== FORGOT PASSWORD ====================

    @Nested
    @DisplayName("POST /api/auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("always returns 200 to prevent email enumeration")
        void forgotPassword_unknownEmail_returns200() throws Exception {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"nonexistent@example.com\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("returns 400 for invalid email format")
        void forgotPassword_invalidEmail_returns400() throws Exception {
            mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"not-valid\"}"))
                    .andExpect(status().isBadRequest());
        }
    }

    // ==================== EMAIL VERIFICATION ====================

    @Nested
    @DisplayName("GET /api/auth/verify-email")
    class VerifyEmail {

        @Test
        @DisplayName("returns 401 for an unknown or invalid token")
        void verifyEmail_invalidToken_returns401() throws Exception {
            mockMvc.perform(get("/api/auth/verify-email")
                            .param("token", "invalid-token-xyz"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ==================== ACTUATOR ====================

    @Nested
    @DisplayName("GET /actuator/health")
    class Actuator {

        @Test
        @DisplayName("health endpoint is publicly accessible without authentication")
        void actuatorHealth_publicAccess() throws Exception {
            mockMvc.perform(get("/actuator/health"))
                    .andExpect(status().isOk());
        }
    }
}

