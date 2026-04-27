package com.research.backend;

import com.research.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    // Must be at least 64 chars for HS256
    private static final String SECRET =
            "test-secret-key-that-is-at-least-64-characters-long-for-hs256-algorithm-ok";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 86_400_000L, 604_800_000L);
    }

    @Test
    @DisplayName("Should generate a valid access token and extract username")
    void generateAccessToken_shouldBeValidAndExtractUsername() {
        UserDetails user = buildUser("alice");
        String token = jwtService.generateAccessToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    @DisplayName("Token signed with wrong secret should be invalid")
    void tokenFromDifferentSecret_shouldBeInvalid() {
        JwtService otherService = new JwtService(
                "completely-different-secret-key-64-chars-abcdefghijklmnopqrstuvwx",
                86_400_000L, 604_800_000L);

        UserDetails user = buildUser("bob");
        String tokenFromOtherService = otherService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(tokenFromOtherService, user)).isFalse();
    }

    @Test
    @DisplayName("Token for different user should fail validation")
    void tokenForDifferentUser_shouldFailValidation() {
        UserDetails alice = buildUser("alice");
        UserDetails bob = buildUser("bob");
        String aliceToken = jwtService.generateAccessToken(alice);

        assertThat(jwtService.isTokenValid(aliceToken, bob)).isFalse();
    }

    @Test
    @DisplayName("Refresh token should be generated and valid")
    void generateRefreshToken_shouldBeValid() {
        UserDetails user = buildUser("carol");
        String refreshToken = jwtService.generateRefreshToken(user);

        assertThat(jwtService.extractUsername(refreshToken)).isEqualTo("carol");
        assertThat(jwtService.isTokenValid(refreshToken, "carol")).isTrue();
    }

    private UserDetails buildUser(String username) {
        return User.builder()
                .username(username)
                .password("irrelevant")
                .authorities(Collections.emptyList())
                .build();
    }
}
