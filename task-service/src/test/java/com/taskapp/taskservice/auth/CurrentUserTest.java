package com.taskapp.taskservice.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class CurrentUserTest {

    private static Jwt.Builder token() {
        return Jwt.withTokenValue("t").header("alg", "HS256").subject("42")
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60));
    }

    @Test
    void usesNameClaim() {
        CurrentUser u = CurrentUser.from(token().claim("email", "raju@example.com").claim("name", "Raju").build());
        assertThat(u.id()).isEqualTo(42L);
        assertThat(u.name()).isEqualTo("Raju");
    }

    @Test
    void fallsBackToEmailLocalPartForOldTokens() {
        CurrentUser u = CurrentUser.from(token().claim("email", "raju@example.com").build());
        assertThat(u.name()).isEqualTo("raju");
    }
}
