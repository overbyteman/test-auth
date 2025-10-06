package com.seccreto.service.auth.domain.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InputSanitizerTest {
    @Test
    void testSanitize() {
        assertEquals("abc", InputSanitizer.sanitize("  abc  "));
        assertEquals("abc 123", InputSanitizer.sanitize("abc  123\n"));
        assertEquals("abc", InputSanitizer.sanitize("abc\u0000"));
    }

    @Test
    void testContainsSqlInjection() {
        assertTrue(InputSanitizer.containsSqlInjection("SELECT * FROM users"));
        assertFalse(InputSanitizer.containsSqlInjection("normal text"));
    }

    @Test
    void testContainsXss() {
        assertTrue(InputSanitizer.containsXss("<script>alert('xss')</script>"));
        assertFalse(InputSanitizer.containsXss("normal text"));
    }

    @Test
    void testContainsInjection() {
        assertTrue(InputSanitizer.containsInjection("alert('x')"));
        assertFalse(InputSanitizer.containsInjection("normal text"));
    }

    @Test
    void testIsSafe() {
        assertTrue(InputSanitizer.isSafe("normal text"));
        assertFalse(InputSanitizer.isSafe("SELECT * FROM users"));
        assertFalse(InputSanitizer.isSafe("<script>alert('xss')</script>"));
    }

    @Test
    void testValidateInput() {
        assertDoesNotThrow(() -> InputSanitizer.validateInput("normal text"));
        assertThrows(InputValidationException.class, () -> InputSanitizer.validateInput("SELECT * FROM users"));
        assertThrows(InputValidationException.class, () -> InputSanitizer.validateInput("<script>alert('xss')</script>"));
        assertThrows(InputValidationException.class, () -> InputSanitizer.validateInput("alert('x')"));
    }

    @Test
    void testEscapeHtml() {
    assertEquals("&lt;b&gt;test&lt;&#x2F;b&gt;", InputSanitizer.escapeHtml("<b>test</b>"));
    }

    @Test
    void testEscapeSql() {
        assertEquals("O''Reilly", InputSanitizer.escapeSql("O'Reilly"));
    }

    @Test
    void testEscapeJavaScript() {
        assertEquals("abc\\n", InputSanitizer.escapeJavaScript("abc\n"));
    }

    @Test
    void testPrivateConstructor() {
        Exception ex = assertThrows(Exception.class, () -> {
            java.lang.reflect.Constructor<InputSanitizer> c = InputSanitizer.class.getDeclaredConstructor();
            c.setAccessible(true);
            c.newInstance();
        });
        // InvocationTargetException é esperado, pois encapsula UnsupportedOperationException
        assertTrue(ex instanceof java.lang.reflect.InvocationTargetException);
        assertTrue(ex.getCause() instanceof UnsupportedOperationException);
    }
}
