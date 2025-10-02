package com.seccreto.service.auth.config;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encoder de senhas pós-quântico usando Argon2id.
 * 
 * Características de segurança pós-quântica:
 * - Argon2id: Resistente a ataques de canal lateral e paralelos
 * - Parâmetros otimizados para segurança máxima
 * - Resistente a ataques quânticos conhecidos
 * - Configuração baseada nas recomendações OWASP 2024
 */
@Component
public class PostQuantumPasswordEncoder implements PasswordEncoder {

    private static final Logger logger = LoggerFactory.getLogger(PostQuantumPasswordEncoder.class);
    
    // Configurações Argon2id otimizadas para segurança pós-quântica
    private static final int SALT_LENGTH = 32;      // 256 bits - resistente a ataques quânticos
    private static final int HASH_LENGTH = 64;      // 512 bits - dobro do padrão para segurança extra
    private static final int PARALLELISM = 4;       // Threads paralelas
    private static final int MEMORY_COST = 65536;   // 64MB - custo de memória alto
    private static final int TIME_COST = 3;         // 3 iterações - balanceamento performance/segurança
    
    private final Argon2PasswordEncoder argon2Encoder;
    
    public PostQuantumPasswordEncoder() {
        // Argon2id é a variante mais segura (combina Argon2i + Argon2d)
        this.argon2Encoder = new Argon2PasswordEncoder(
            SALT_LENGTH,
            HASH_LENGTH, 
            PARALLELISM,
            MEMORY_COST,
            TIME_COST
        );
        
        logger.info("Initialized Post-Quantum Password Encoder with Argon2id");
        logger.info("Configuration: saltLength={}, hashLength={}, parallelism={}, memoryCost={}KB, timeCost={}", 
            SALT_LENGTH, HASH_LENGTH, PARALLELISM, MEMORY_COST, TIME_COST);
    }
    
    /**
     * Codifica uma senha usando Argon2id com parâmetros pós-quânticos
     */
    @Override
    public String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("Raw password cannot be null");
        }
        
        long startTime = System.currentTimeMillis();
        String encodedPassword = argon2Encoder.encode(rawPassword);
        long encodingTime = System.currentTimeMillis() - startTime;
        
        // Log tempo de encoding para monitoramento (sem expor a senha)
        logger.debug("Password encoded in {}ms using Argon2id", encodingTime);
        
        // Verificar se o hash tem o prefixo correto do Argon2id
        if (!encodedPassword.startsWith("$argon2id$")) {
            logger.error("Generated hash does not have correct Argon2id prefix");
            throw new IllegalStateException("Invalid Argon2id hash generated");
        }
        
        return encodedPassword;
    }
    
    /**
     * Verifica se uma senha corresponde ao hash Argon2id
     */
    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        if (rawPassword == null) {
            logger.warn("Attempted to match null raw password");
            return false;
        }
        
        if (encodedPassword == null || encodedPassword.trim().isEmpty()) {
            logger.warn("Attempted to match against null or empty encoded password");
            return false;
        }
        
        // Verificar se é um hash Argon2id válido
        if (!encodedPassword.startsWith("$argon2id$")) {
            logger.warn("Attempted to match against non-Argon2id hash: {}", 
                encodedPassword.substring(0, Math.min(20, encodedPassword.length())));
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        boolean matches = argon2Encoder.matches(rawPassword, encodedPassword);
        long verificationTime = System.currentTimeMillis() - startTime;
        
        logger.debug("Password verification completed in {}ms, result: {}", verificationTime, matches);
        
        return matches;
    }
    
    /**
     * Verifica se o hash precisa ser atualizado (sempre false para Argon2id)
     * Argon2id é considerado seguro para o futuro pós-quântico
     */
    @Override
    public boolean upgradeEncoding(String encodedPassword) {
        // Argon2id com nossos parâmetros é considerado seguro pós-quântico
        // Só atualizaria se mudássemos os parâmetros para versões futuras
        return false;
    }
    
    /**
     * Valida se um hash é do formato Argon2id esperado
     */
    public boolean isValidArgon2Hash(String hash) {
        if (hash == null || hash.trim().isEmpty()) {
            return false;
        }
        
        // Formato esperado: $argon2id$v=19$m=65536,t=3,p=4$salt$hash
        return hash.startsWith("$argon2id$v=19$") && 
               hash.contains("m=" + MEMORY_COST) &&
               hash.contains("t=" + TIME_COST) &&
               hash.contains("p=" + PARALLELISM);
    }
    
    /**
     * Retorna informações sobre a configuração atual
     */
    public String getConfigurationInfo() {
        return String.format(
            "Argon2id Post-Quantum Configuration: " +
            "saltLength=%d bits, hashLength=%d bits, " +
            "parallelism=%d, memoryCost=%d KB, timeCost=%d iterations",
            SALT_LENGTH * 8, HASH_LENGTH * 8, 
            PARALLELISM, MEMORY_COST, TIME_COST
        );
    }
}
