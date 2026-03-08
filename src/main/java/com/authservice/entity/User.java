package com.authservice.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private boolean enabled = false;
    private boolean locked = false;
    private Instant lockedUntil;
    private int failedAttempts = 0;
    private boolean mfaEnabled = false;
    private String mfaSecret;
    private boolean emailVerified = false;
    private String verificationToken;
    private Instant verificationTokenExpires;
    private String passwordResetToken;
    private Instant passwordResetExpires;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    public User() {
    }

    public User(UUID id, String username, String email, String password, boolean enabled, boolean locked,
                Instant lockedUntil, int failedAttempts, boolean mfaEnabled, String mfaSecret,
                boolean emailVerified, String verificationToken, Instant verificationTokenExpires,
                String passwordResetToken, Instant passwordResetExpires, Instant createdAt,
                Instant updatedAt, Set<Role> roles) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.enabled = enabled;
        this.locked = locked;
        this.lockedUntil = lockedUntil;
        this.failedAttempts = failedAttempts;
        this.mfaEnabled = mfaEnabled;
        this.mfaSecret = mfaSecret;
        this.emailVerified = emailVerified;
        this.verificationToken = verificationToken;
        this.verificationTokenExpires = verificationTokenExpires;
        this.passwordResetToken = passwordResetToken;
        this.passwordResetExpires = passwordResetExpires;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.roles = roles != null ? roles : new HashSet<>();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    // Getters and Setters
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public void setLockedUntil(Instant lockedUntil) {
        this.lockedUntil = lockedUntil;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public void setFailedAttempts(int failedAttempts) {
        this.failedAttempts = failedAttempts;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public String getMfaSecret() {
        return mfaSecret;
    }

    public void setMfaSecret(String mfaSecret) {
        this.mfaSecret = mfaSecret;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public Instant getVerificationTokenExpires() {
        return verificationTokenExpires;
    }

    public void setVerificationTokenExpires(Instant verificationTokenExpires) {
        this.verificationTokenExpires = verificationTokenExpires;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public Instant getPasswordResetExpires() {
        return passwordResetExpires;
    }

    public void setPasswordResetExpires(Instant passwordResetExpires) {
        this.passwordResetExpires = passwordResetExpires;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public void setRoles(Set<Role> roles) {
        this.roles = roles;
    }

    // Builder
    public static UserBuilder builder() {
        return new UserBuilder();
    }

    public static class UserBuilder {
        private UUID id;
        private String username;
        private String email;
        private String password;
        private boolean enabled = false;
        private boolean locked = false;
        private Instant lockedUntil;
        private int failedAttempts = 0;
        private boolean mfaEnabled = false;
        private String mfaSecret;
        private boolean emailVerified = false;
        private String verificationToken;
        private Instant verificationTokenExpires;
        private String passwordResetToken;
        private Instant passwordResetExpires;
        private Instant createdAt;
        private Instant updatedAt;
        private Set<Role> roles = new HashSet<>();

        public UserBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserBuilder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public UserBuilder locked(boolean locked) {
            this.locked = locked;
            return this;
        }

        public UserBuilder lockedUntil(Instant lockedUntil) {
            this.lockedUntil = lockedUntil;
            return this;
        }

        public UserBuilder failedAttempts(int failedAttempts) {
            this.failedAttempts = failedAttempts;
            return this;
        }

        public UserBuilder mfaEnabled(boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
            return this;
        }

        public UserBuilder mfaSecret(String mfaSecret) {
            this.mfaSecret = mfaSecret;
            return this;
        }

        public UserBuilder emailVerified(boolean emailVerified) {
            this.emailVerified = emailVerified;
            return this;
        }

        public UserBuilder verificationToken(String verificationToken) {
            this.verificationToken = verificationToken;
            return this;
        }

        public UserBuilder verificationTokenExpires(Instant verificationTokenExpires) {
            this.verificationTokenExpires = verificationTokenExpires;
            return this;
        }

        public UserBuilder passwordResetToken(String passwordResetToken) {
            this.passwordResetToken = passwordResetToken;
            return this;
        }

        public UserBuilder passwordResetExpires(Instant passwordResetExpires) {
            this.passwordResetExpires = passwordResetExpires;
            return this;
        }

        public UserBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserBuilder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public UserBuilder roles(Set<Role> roles) {
            this.roles = roles;
            return this;
        }

        public User build() {
            return new User(id, username, email, password, enabled, locked, lockedUntil, failedAttempts,
                    mfaEnabled, mfaSecret, emailVerified, verificationToken, verificationTokenExpires,
                    passwordResetToken, passwordResetExpires, createdAt, updatedAt, roles);
        }
    }
}
