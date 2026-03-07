package com.authservice.controller;

import com.authservice.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController {

    @Value("${jwt.public-key}")
    private String publicKeyPem;

    /**
     * Exposes the JWKS endpoint so resource servers can verify JWTs.
     */
    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() throws Exception {
        // Parse PEM public key
        String keyStr = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(keyStr);
        java.security.spec.X509EncodedKeySpec spec = new java.security.spec.X509EncodedKeySpec(keyBytes);
        RSAPublicKey rsaPublicKey = (RSAPublicKey) java.security.KeyFactory.getInstance("RSA").generatePublic(spec);

        String n = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(rsaPublicKey.getModulus().toByteArray());
        String e = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(rsaPublicKey.getPublicExponent().toByteArray());

        Map<String, String> key = Map.of(
                "kty", "RSA",
                "use", "sig",
                "alg", "RS256",
                "n", n,
                "e", e,
                "kid", "auth-service-key-1"
        );

        return Map.of("keys", List.of(key));
    }
}
