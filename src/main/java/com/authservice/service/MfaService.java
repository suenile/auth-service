package com.authservice.service;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MfaService {

    private static final Logger log = LoggerFactory.getLogger(MfaService.class);
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
