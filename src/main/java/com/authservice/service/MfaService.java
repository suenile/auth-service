package com.authservice.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MfaService {

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    public String generateSecret() {
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        return key.getKey();
    }

    public String generateQrCodeUrl(String secret, String email, String issuer) {
        GoogleAuthenticatorKey key = new GoogleAuthenticatorKey.Builder(secret).build();
        return GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(issuer, email, key);
    }

    public boolean verifyCode(String secret, int code) {
        return googleAuthenticator.authorize(secret, code);
    }

    public boolean verifyCode(String secret, String codeStr) {
        try {
            int code = Integer.parseInt(codeStr);
            return verifyCode(secret, code);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
