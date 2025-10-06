package com.seccreto.service.auth.api.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvalidTokenExceptionTest {
    @Test
    void testMessage() {
        InvalidTokenException ex = new InvalidTokenException("token inválido");
        assertEquals("token inválido", ex.getMessage());
    }

    @Test
    void testMessageAndCause() {
        Throwable cause = new RuntimeException("causa");
        InvalidTokenException ex = new InvalidTokenException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
