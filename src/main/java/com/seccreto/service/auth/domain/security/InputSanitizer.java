package com.seccreto.service.auth.domain.security;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Utilitário para sanitização de entrada de dados
 * Previne SQL injection, XSS e outros ataques de injeção
 */
public class InputSanitizer {

    // Padrões perigosos para SQL injection
    private static final Pattern[] SQL_INJECTION_PATTERNS = {
            Pattern.compile("(?i).*\\b(union|select|insert|update|delete|drop|create|alter|exec|execute)\\b.*"),
            Pattern.compile("(?i).*\\b(or|and)\\s+\\d+\\s*=\\s*\\d+.*"),
            Pattern.compile("(?i).*\\b(or|and)\\s+'.*'\\s*=\\s*'.*'.*"),
            Pattern.compile("(?i).*\\b(union|select).*\\b(from|where|group|order|having)\\b.*"),
            Pattern.compile("(?i).*\\b(union|select).*\\b(concat|substring|char|ascii|hex|unhex)\\b.*"),
            Pattern.compile("(?i).*\\b(union|select).*\\b(information_schema|mysql|sys|performance_schema)\\b.*"),
            Pattern.compile("(?i).*\\b(union|select).*\\b(tables|columns|users|databases)\\b.*"),
            Pattern.compile("(?i).*\\b(union|select).*\\b(version|user|database|schema)\\b.*"),
            Pattern.compile("(?i).*\\b(union|select).*\\b(load_file|into\\s+outfile|into\\s+dumpfile)\\b.*"),
            Pattern.compile("(?i).*\\b(union|select).*\\b(load_file|into\\s+outfile|into\\s+dumpfile)\\b.*")
    };

    // Padrões perigosos para XSS
    private static final Pattern[] XSS_PATTERNS = {
            Pattern.compile("(?i).*<script.*>.*</script>.*"),
            Pattern.compile("(?i).*<script.*>.*"),
            Pattern.compile("(?i).*javascript:.*"),
            Pattern.compile("(?i).*vbscript:.*"),
            Pattern.compile("(?i).*onload\\s*=.*"),
            Pattern.compile("(?i).*onerror\\s*=.*"),
            Pattern.compile("(?i).*onclick\\s*=.*"),
            Pattern.compile("(?i).*onmouseover\\s*=.*"),
            Pattern.compile("(?i).*onfocus\\s*=.*"),
            Pattern.compile("(?i).*onblur\\s*=.*"),
            Pattern.compile("(?i).*onchange\\s*=.*"),
            Pattern.compile("(?i).*onsubmit\\s*=.*"),
            Pattern.compile("(?i).*onreset\\s*=.*"),
            Pattern.compile("(?i).*onselect\\s*=.*"),
            Pattern.compile("(?i).*onkeydown\\s*=.*"),
            Pattern.compile("(?i).*onkeyup\\s*=.*"),
            Pattern.compile("(?i).*onkeypress\\s*=.*"),
            Pattern.compile("(?i).*onmousedown\\s*=.*"),
            Pattern.compile("(?i).*onmouseup\\s*=.*"),
            Pattern.compile("(?i).*onmousemove\\s*=.*"),
            Pattern.compile("(?i).*onmouseout\\s*=.*"),
            Pattern.compile("(?i).*oncontextmenu\\s*=.*"),
            Pattern.compile("(?i).*ondblclick\\s*=.*"),
            Pattern.compile("(?i).*onabort\\s*=.*"),
            Pattern.compile("(?i).*onbeforeunload\\s*=.*"),
            Pattern.compile("(?i).*onerror\\s*=.*"),
            Pattern.compile("(?i).*onhashchange\\s*=.*"),
            Pattern.compile("(?i).*onload\\s*=.*"),
            Pattern.compile("(?i).*onmessage\\s*=.*"),
            Pattern.compile("(?i).*onoffline\\s*=.*"),
            Pattern.compile("(?i).*ononline\\s*=.*"),
            Pattern.compile("(?i).*onpagehide\\s*=.*"),
            Pattern.compile("(?i).*onpageshow\\s*=.*"),
            Pattern.compile("(?i).*onpopstate\\s*=.*"),
            Pattern.compile("(?i).*onresize\\s*=.*"),
            Pattern.compile("(?i).*onstorage\\s*=.*"),
            Pattern.compile("(?i).*onunload\\s*=.*"),
            Pattern.compile("(?i).*onbeforeprint\\s*=.*"),
            Pattern.compile("(?i).*onafterprint\\s*=.*"),
            Pattern.compile("(?i).*oncanplay\\s*=.*"),
            Pattern.compile("(?i).*oncanplaythrough\\s*=.*"),
            Pattern.compile("(?i).*ondurationchange\\s*=.*"),
            Pattern.compile("(?i).*onemptied\\s*=.*"),
            Pattern.compile("(?i).*onended\\s*=.*"),
            Pattern.compile("(?i).*onerror\\s*=.*"),
            Pattern.compile("(?i).*onloadeddata\\s*=.*"),
            Pattern.compile("(?i).*onloadedmetadata\\s*=.*"),
            Pattern.compile("(?i).*onloadstart\\s*=.*"),
            Pattern.compile("(?i).*onpause\\s*=.*"),
            Pattern.compile("(?i).*onplay\\s*=.*"),
            Pattern.compile("(?i).*onplaying\\s*=.*"),
            Pattern.compile("(?i).*onprogress\\s*=.*"),
            Pattern.compile("(?i).*onratechange\\s*=.*"),
            Pattern.compile("(?i).*onseeked\\s*=.*"),
            Pattern.compile("(?i).*onseeking\\s*=.*"),
            Pattern.compile("(?i).*onstalled\\s*=.*"),
            Pattern.compile("(?i).*onsuspend\\s*=.*"),
            Pattern.compile("(?i).*ontimeupdate\\s*=.*"),
            Pattern.compile("(?i).*onvolumechange\\s*=.*"),
            Pattern.compile("(?i).*onwaiting\\s*=.*")
    };

    // Padrões perigosos para outros tipos de injeção
    private static final Pattern[] INJECTION_PATTERNS = {
            Pattern.compile("(?i).*\\b(eval|function|alert|confirm|prompt)\\s*\\(.*"),
            Pattern.compile("(?i).*\\b(document|window|location|history|navigator)\\b.*"),
            Pattern.compile("(?i).*\\b(cookie|localStorage|sessionStorage)\\b.*"),
            Pattern.compile("(?i).*\\b(xmlhttprequest|fetch|ajax)\\b.*"),
            Pattern.compile("(?i).*\\b(iframe|object|embed|applet)\\b.*"),
            Pattern.compile("(?i).*\\b(form|input|textarea|select|option)\\b.*"),
            Pattern.compile("(?i).*\\b(style|link|meta|base)\\b.*"),
            Pattern.compile("(?i).*\\b(img|video|audio|source)\\b.*"),
            Pattern.compile("(?i).*\\b(canvas|svg|math)\\b.*"),
            Pattern.compile("(?i).*\\b(link|a|area)\\b.*")
    };

    /**
     * Sanitiza uma string de entrada removendo caracteres perigosos
     */
    public static String sanitize(String input) {
        if (input == null || input.trim().isEmpty()) {
            return input;
        }

        String sanitized = input.trim();

        // Remover caracteres de controle
        sanitized = sanitized.replaceAll("[\\x00-\\x1F\\x7F]", "");

        // Remover caracteres especiais perigosos
        sanitized = sanitized.replaceAll("[<>\"'&]", "");

        // Remover espaços em branco excessivos
        sanitized = sanitized.replaceAll("\\s+", " ");

        return sanitized;
    }

    /**
     * Verifica se uma string contém padrões de SQL injection
     */
    public static boolean containsSqlInjection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        for (Pattern pattern : SQL_INJECTION_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se uma string contém padrões de XSS
     */
    public static boolean containsXss(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        for (Pattern pattern : XSS_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se uma string contém padrões de injeção
     */
    public static boolean containsInjection(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(input).matches()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Verifica se uma string é segura (não contém padrões perigosos)
     */
    public static boolean isSafe(String input) {
        if (input == null || input.trim().isEmpty()) {
            return true;
        }

        return !containsSqlInjection(input) &&
                !containsXss(input) &&
                !containsInjection(input);
    }

    /**
     * Valida entrada e lança exceção se não for segura
     */
    public static void validateInput(String input) {
        if (input == null || input.trim().isEmpty()) {
            return;
        }

        if (containsSqlInjection(input)) {
            throw InputValidationException.sqlInjectionDetected(input);
        }

        if (containsXss(input)) {
            throw InputValidationException.xssDetected(input);
        }

        if (containsInjection(input)) {
            throw InputValidationException.injectionDetected(input);
        }
    }

    /**
     * Escapa caracteres especiais para HTML
     */
    public static String escapeHtml(String input) {
        if (input == null) {
            return null;
        }

        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");
    }

    /**
     * Escapa caracteres especiais para SQL
     */
    public static String escapeSql(String input) {
        if (input == null) {
            return null;
        }

        return input.replace("'", "''")
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /**
     * Escapa caracteres especiais para JavaScript
     */
    public static String escapeJavaScript(String input) {
        if (input == null) {
            return null;
        }

        return input.replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
                .replace("\b", "\\b")
                .replace("\f", "\\f");
    }

    // Construtor privado para classe utilitária
    private InputSanitizer() {
        throw new UnsupportedOperationException("Utility class");
    }
}
