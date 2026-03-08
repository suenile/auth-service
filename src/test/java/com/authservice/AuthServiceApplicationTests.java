package com.authservice;

import com.authservice.util.TestRsaKeys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    @DynamicPropertySource
    static void overrideJwtKeys(DynamicPropertyRegistry registry) {
        registry.add("jwt.private-key", TestRsaKeys::privateKeyPem);
        registry.add("jwt.public-key", TestRsaKeys::publicKeyPem);
    }

    @Test
    void contextLoads() {
    }
}
