package com.seccreto.service.auth.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHashGeneratorTest {

    @Test
    void generateHashForChangeMeNow() {
        PostQuantumPasswordEncoder encoder = new PostQuantumPasswordEncoder();
        String rawPassword = System.getProperty("password", "ChangeMeNow!123");
        String hash = encoder.encode(rawPassword);
        System.out.println("Encoded password for '" + rawPassword + "': " + hash);
        assertTrue(encoder.matches(rawPassword, hash));
    }
}
