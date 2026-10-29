package com.gjsk.mediagateway.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordHasherTest {

    private static final String ENCODED_ADMIN123 =
            "pbkdf2$210000$bWVkaWEtZ2F0ZXdheS1zYWx0$7YXHFXaBJFnqQtT/+1n2btlzG6lkQQd2r3G90lzYXH8=";

    @Test
    void shouldMatchKnownVector() {
        assertThat(PasswordHasher.matches("admin123", ENCODED_ADMIN123)).isTrue();
    }

    @Test
    void shouldRejectWrongPassword() {
        assertThat(PasswordHasher.matches("wrongpass", ENCODED_ADMIN123)).isFalse();
    }

    @Test
    void shouldRejectMalformedHash() {
        assertThat(PasswordHasher.matches("admin123", null)).isFalse();
        assertThat(PasswordHasher.matches("admin123", "")).isFalse();
        assertThat(PasswordHasher.matches("admin123", "pbkdf2$bad")).isFalse();
        assertThat(PasswordHasher.matches("admin123", "plain$1$abc$def")).isFalse();
        assertThat(PasswordHasher.matches("admin123", "pbkdf2$0$YWJj$ZGVm")).isFalse();
    }

    @Test
    void hashShouldBeVerifiableAndSalted() {
        String first = PasswordHasher.hash("secret");
        String second = PasswordHasher.hash("secret");

        assertThat(first).startsWith("pbkdf2$210000$").doesNotContain("secret");
        assertThat(first).isNotEqualTo(second);
        assertThat(PasswordHasher.matches("secret", first)).isTrue();
        assertThat(PasswordHasher.matches("secret", second)).isTrue();
    }

    @Test
    void hashShouldRejectEmptyPassword() {
        assertThatThrownBy(() -> PasswordHasher.hash(""))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
