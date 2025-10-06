package com.seccreto.service.auth.api.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InsufficientPermissionsExceptionTest {
    @Test
    void testMessage() {
        InsufficientPermissionsException ex = new InsufficientPermissionsException("sem permissão");
        assertEquals("sem permissão", ex.getMessage());
    }

    @Test
    void testMessageAndCause() {
        Throwable cause = new RuntimeException("causa");
        InsufficientPermissionsException ex = new InsufficientPermissionsException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
