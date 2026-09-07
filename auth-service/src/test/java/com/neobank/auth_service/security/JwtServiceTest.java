package com.neobank.auth_service.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm",
                3600000L
        );
    }

    @Test
    void generateToken_containsUsernameAndCustomerId() {
        String token = jwtService.generateToken("john", "CUS-A82F91BC");
        assertThat(jwtService.extractUsername(token)).isEqualTo("john");
        assertThat(jwtService.extractCustomerId(token)).isEqualTo("CUS-A82F91BC");
    }

    @Test
    void generateToken_doesNotContainRole() {
        String token = jwtService.generateToken("john", "CUS-A82F91BC");
        // JWT payload should not contain "role"
        String[] parts = token.split("\\.");
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        assertThat(payload).doesNotContain("role");
        assertThat(payload).doesNotContain("accountId");
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken("john", "CUS-A82F91BC");
        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() {
        JwtService shortLived = new JwtService(
                "test-secret-key-that-is-long-enough-for-hmac-sha256-algorithm",
                -1000L
        );
        String token = shortLived.generateToken("john", "CUS-A82F91BC");
        assertThat(shortLived.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_invalidSignature_returnsFalse() {
        String token = jwtService.generateToken("john", "CUS-A82F91BC");
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_malformedToken_returnsFalse() {
        assertThat(jwtService.isTokenValid("not.a.jwt")).isFalse();
    }
}
