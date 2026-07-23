package com.petcare.backend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.petcare.backend.model.User;
import io.jsonwebtoken.JwtException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class JwtServiceTest {
    private static final String SECRET = "unit-test-secret-key-must-have-at-least-thirty-two-bytes";
    private UserPrincipal principal(String email) { User u=new User();u.setId(7L);u.setEmail(email);u.setPasswordHash("hash");u.setStatus("active");return UserPrincipal.from(u); }

    @Test void generateExtractAndValidateToken_UsesPrincipalSubjectAndClaims() {
        JwtService service=new JwtService(SECRET,TimeUnit.MINUTES.toMillis(1));UserPrincipal principal=principal("user@test.com");String token=service.generateToken(principal);
        assertThat(service.extractUsername(token)).isEqualTo("user@test.com");assertThat(service.isTokenValid(token,principal)).isTrue();assertThat(service.isTokenValid(token,principal("other@test.com"))).isFalse();
    }
    @Test void expiredMalformedAndWrongSecretTokens_AreRejected() throws InterruptedException {
        JwtService expired=new JwtService(SECRET,-1);String expiredToken=expired.generateToken(principal("user@test.com"));assertThatThrownBy(()->expired.isTokenValid(expiredToken,principal("user@test.com"))).isInstanceOf(JwtException.class);
        JwtService different=new JwtService("another-unit-test-secret-key-with-thirty-two-bytes",1000);assertThatThrownBy(()->different.extractUsername(expiredToken)).isInstanceOf(JwtException.class);assertThatThrownBy(()->expired.extractUsername("not.a.jwt")).isInstanceOf(JwtException.class);
    }
}
