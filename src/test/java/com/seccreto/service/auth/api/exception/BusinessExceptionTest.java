package com.seccreto.service.auth.api.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BusinessExceptionTest {
    @Test
    void testMessage() {
        BusinessException ex = new BusinessException("erro de negócio");
        assertEquals("erro de negócio", ex.getMessage());
    }

    @Test
    void testMessageAndCause() {
        Throwable cause = new RuntimeException("causa");
        BusinessException ex = new BusinessException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
