package com.authservice.service;

import com.authservice.repository.RefreshTokenRepository;
import com.authservice.repository.TokenBlacklistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;

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
