package com.authservice.util;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

/**
 * Lazily generates a single RSA-2048 key pair for the entire test suite.
 * Avoids the overhead of generating keys per test class.
 */
public final class TestRsaKeys {

    private static KeyPair KEY_PAIR;

    static {
        try {
            KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
            gen.initialize(2048);
            KEY_PAIR = gen.generateKeyPair();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private TestRsaKeys() {}

    public static PrivateKey privateKey() {
        return KEY_PAIR.getPrivate();
    }

    public static PublicKey publicKey() {
        return KEY_PAIR.getPublic();
    }

    /** Returns a PEM-formatted PKCS#8 private key string. */
    public static String privateKeyPem() {
        String b64 = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(KEY_PAIR.getPrivate().getEncoded());
        return "-----BEGIN PRIVATE KEY-----\n" + b64 + "\n-----END PRIVATE KEY-----";
    }

    /** Returns a PEM-formatted X.509 public key string. */
    public static String publicKeyPem() {
        String b64 = Base64.getMimeEncoder(64, new byte[]{'\n'})
                .encodeToString(KEY_PAIR.getPublic().getEncoded());
        return "-----BEGIN PUBLIC KEY-----\n" + b64 + "\n-----END PUBLIC KEY-----";
    }
}
