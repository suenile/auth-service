package com.authservice.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "token_blacklist")
public class TokenBlacklist {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String jti;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public TokenBlacklist() {
    }

    public TokenBlacklist(UUID id, String jti, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.jti = jti;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getJti() {
        return jti;
    }

    public void setJti(String jti) {
        this.jti = jti;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static TokenBlacklistBuilder builder() {
        return new TokenBlacklistBuilder();
    }

    public static class TokenBlacklistBuilder {
        private UUID id;
        private String jti;
        private Instant expiresAt;
        private Instant createdAt;

        public TokenBlacklistBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public TokenBlacklistBuilder jti(String jti) {
            this.jti = jti;
            return this;
        }

        public TokenBlacklistBuilder expiresAt(Instant expiresAt) {
            this.expiresAt = expiresAt;
            return this;
        }

        public TokenBlacklistBuilder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public TokenBlacklist build() {
            return new TokenBlacklist(id, jti, expiresAt, createdAt);
        }
    }
}
