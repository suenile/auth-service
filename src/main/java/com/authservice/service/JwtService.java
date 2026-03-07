package com.authservice.service;

import com.authservice.entity.TokenBlacklist;
import com.authservice.entity.User;
import com.authservice.repository.TokenBlacklistRepository;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${jwt.private-key}")
    private String privateKeyPem;

    @Value("${jwt.public-key}")
    private String publicKeyPem;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.issuer}")
    private String issuer;

    private PrivateKey privateKey;
    private PublicKey publicKey;

    private final TokenBlacklistRepository tokenBlacklistRepository;

    @PostConstruct
    public void initKeys() throws Exception {
        String privKeyStr = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] privKeyBytes = Base64.getDecoder().decode(privKeyStr);
        PKCS8EncodedKeySpec privKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        privateKey = kf.generatePrivate(privKeySpec);

        String pubKeyStr = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] pubKeyBytes = Base64.getDecoder().decode(pubKeyStr);
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
        publicKey = kf.generatePublic(pubKeySpec);
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        String roles = user.getRoles().stream()
                .map(r -> r.getName())
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(user.getId().toString())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpiration)))
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("roles", roles)
                .signWith(privateKey, Jwts.SIG.RS256)
                .compact();
    }

    public Claims validateAndParseClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = validateAndParseClaims(token);
            String jti = claims.getId();
            return !tokenBlacklistRepository.existsByJti(jti);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String getSubject(String token) {
        return validateAndParseClaims(token).getSubject();
    }

    public String getJti(String token) {
        return validateAndParseClaims(token).getId();
    }

    public Instant getExpiration(String token) {
        return validateAndParseClaims(token).getExpiration().toInstant();
    }

    @Transactional
    public void blacklistToken(String token) {
        try {
            Claims claims = validateAndParseClaims(token);
            TokenBlacklist entry = TokenBlacklist.builder()
                    .jti(claims.getId())
                    .expiresAt(claims.getExpiration().toInstant())
                    .build();
            tokenBlacklistRepository.save(entry);
        } catch (JwtException e) {
            log.warn("Tried to blacklist invalid token: {}", e.getMessage());
        }
    }
}
