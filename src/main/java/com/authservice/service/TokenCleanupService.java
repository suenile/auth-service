package com.authservice.service;

import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.TokenBlacklistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TokenCleanupService {

    private static final Logger log = LoggerFactory.getLogger(TokenCleanupService.class);
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    public TokenCleanupService(RefreshTokenRepository refreshTokenRepository,
                               TokenBlacklistRepository tokenBlacklistRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
    }

    @Scheduled(cron = "0 0 * * * *") // every hour
    @Transactional
    public void cleanupTokens() {
        log.info("Running token cleanup...");
        Instant now = Instant.now();
        refreshTokenRepository.deleteExpiredAndRevoked(now);
        tokenBlacklistRepository.deleteExpired(now);
        log.info("Token cleanup completed.");
    }
}
