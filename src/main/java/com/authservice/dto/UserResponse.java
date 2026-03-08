package com.authservice.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private boolean enabled;
    private boolean mfaEnabled;
    private boolean emailVerified;
    private Set<String> roles;
    private Instant createdAt;

    public UserResponse() {
    }

    public UserResponse(UUID id, String username, String email, boolean enabled, boolean mfaEnabled,
                        boolean emailVerified, Set<String> roles, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.enabled = enabled;
        this.mfaEnabled = mfaEnabled;
        this.emailVerified = emailVerified;
        this.roles = roles;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static UserResponseBuilder builder() {
        return new UserResponseBuilder();
    }

    public static class UserResponseBuilder {
        private UUID id;
        private String username;
        private String email;
        private boolean enabled;
        private boolean mfaEnabled;
        private boolean emailVerified;
        private Set<String> roles;
        private Instant createdAt;

        public UserResponseBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public UserResponseBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserResponseBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserResponseBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserResponseBuilder mfaEnabled(boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
            return this;
        }

        public UserResponseBuilder emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public UserResponseBuilder roles(Set<String> roles) {
            this.roles = roles;
            return this;
        }

        public UserResponseBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserResponse build() {
            return new UserResponse(id, username, email, enabled, mfaEnabled, emailVerified, roles, createdAt);
        }
    }
}
