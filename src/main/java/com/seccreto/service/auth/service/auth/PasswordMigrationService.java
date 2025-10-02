package com.seccreto.service.auth.service.auth;

import com.seccreto.service.auth.config.PostQuantumPasswordEncoder;
import com.seccreto.service.auth.model.users.User;
import com.seccreto.service.auth.repository.users.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Serviço para migração de senhas de BCrypt para Argon2id pós-quântico.
 * 
 * Funcionalidades:
 * - Detecção automática do tipo de hash
 * - Migração transparente durante login
 * - Migração em lote para usuários existentes
 * - Compatibilidade com hashes antigos
 */
@Service
public class PasswordMigrationService {

    private static final Logger logger = LoggerFactory.getLogger(PasswordMigrationService.class);
    
    private final PostQuantumPasswordEncoder postQuantumEncoder;
    private final BCryptPasswordEncoder legacyBCryptEncoder;
    private final UserRepository userRepository;
    
    public PasswordMigrationService(PostQuantumPasswordEncoder postQuantumEncoder,
                                  UserRepository userRepository) {
        this.postQuantumEncoder = postQuantumEncoder;
        this.legacyBCryptEncoder = new BCryptPasswordEncoder();
        this.userRepository = userRepository;
    }
    
    /**
     * Verifica senha e migra automaticamente se necessário
     */
    public boolean verifyAndMigratePassword(String rawPassword, String storedHash, UUID userId) {
        if (rawPassword == null || storedHash == null) {
            return false;
        }
        
        // Verificar se já é Argon2id
        if (isArgon2Hash(storedHash)) {
            logger.debug("Password already using Argon2id for user: {}", userId);
            return postQuantumEncoder.matches(rawPassword, storedHash);
        }
        
        // Verificar se é BCrypt legado
        if (isBCryptHash(storedHash)) {
            logger.debug("Detected legacy BCrypt hash for user: {}", userId);
            
            if (legacyBCryptEncoder.matches(rawPassword, storedHash)) {
                // Senha correta, migrar para Argon2id
                migrateUserPassword(userId, rawPassword);
                return true;
            }
            return false;
        }
        
        // Hash desconhecido
        logger.warn("Unknown password hash format for user: {}", userId);
        return false;
    }
    
    /**
     * Migra a senha de um usuário para Argon2id
     */
    @Transactional
    public void migrateUserPassword(UUID userId, String rawPassword) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            String newHash = postQuantumEncoder.encode(rawPassword);
            user.setPasswordHash(newHash);
            user.updateTimestamp();
            
            userRepository.save(user);
            
            logger.info("Successfully migrated password to Argon2id for user: {}", userId);
            
        } catch (Exception e) {
            logger.error("Failed to migrate password for user: {}", userId, e);
            throw new RuntimeException("Password migration failed", e);
        }
    }
    
    /**
     * Migra todas as senhas BCrypt para Argon2id
     * ATENÇÃO: Este método só funciona se você tiver as senhas em texto claro
     * Em um cenário real, a migração acontece durante o login
     */
    @Transactional
    public void migrateAllLegacyPasswords() {
        logger.info("Starting migration of legacy BCrypt passwords to Argon2id");
        
        List<User> usersWithBCrypt = userRepository.findAll().stream()
                .filter(user -> isBCryptHash(user.getPasswordHash()))
                .toList();
        
        logger.info("Found {} users with legacy BCrypt passwords", usersWithBCrypt.size());
        
        if (usersWithBCrypt.isEmpty()) {
            logger.info("No legacy passwords found to migrate");
            return;
        }
        
        logger.warn("Cannot migrate existing BCrypt hashes without plaintext passwords");
        logger.info("Migration will happen automatically when users log in");
        
        // Em um ambiente real, você poderia:
        // 1. Forçar reset de senha para todos os usuários
        // 2. Migrar durante o próximo login (implementado no verifyAndMigratePassword)
        // 3. Manter compatibilidade com ambos os formatos
    }
    
    /**
     * Força reset de senha para usuários com hashes legados
     */
    @Transactional
    public void forcePasswordResetForLegacyUsers() {
        List<User> usersWithBCrypt = userRepository.findAll().stream()
                .filter(user -> isBCryptHash(user.getPasswordHash()))
                .toList();
        
        logger.info("Forcing password reset for {} users with legacy hashes", usersWithBCrypt.size());
        
        for (User user : usersWithBCrypt) {
            // Marcar usuário como precisando redefinir senha
            user.setActive(false); // Desativar até redefinir senha
            user.updateTimestamp();
            userRepository.save(user);
            
            logger.info("Forced password reset for user: {}", user.getId());
        }
        
        logger.info("Completed forcing password reset for legacy users");
    }
    
    /**
     * Verifica se um hash é do formato Argon2id
     */
    private boolean isArgon2Hash(String hash) {
        return hash != null && hash.startsWith("$argon2id$");
    }
    
    /**
     * Verifica se um hash é do formato BCrypt
     */
    private boolean isBCryptHash(String hash) {
        return hash != null && (hash.startsWith("$2a$") || hash.startsWith("$2b$") || hash.startsWith("$2y$"));
    }
    
    /**
     * Retorna estatísticas de migração
     */
    public MigrationStats getMigrationStats() {
        List<User> allUsers = userRepository.findAll();
        
        long argon2Count = allUsers.stream()
                .filter(user -> isArgon2Hash(user.getPasswordHash()))
                .count();
        
        long bcryptCount = allUsers.stream()
                .filter(user -> isBCryptHash(user.getPasswordHash()))
                .count();
        
        long unknownCount = allUsers.size() - argon2Count - bcryptCount;
        
        return new MigrationStats(
            allUsers.size(),
            argon2Count,
            bcryptCount,
            unknownCount,
            (double) argon2Count / allUsers.size() * 100
        );
    }
    
    /**
     * Classe para estatísticas de migração
     */
    public record MigrationStats(
        long totalUsers,
        long argon2Users,
        long bcryptUsers,
        long unknownUsers,
        double migrationPercentage
    ) {
        @Override
        public String toString() {
            return String.format(
                "Migration Stats: Total=%d, Argon2id=%d (%.1f%%), BCrypt=%d, Unknown=%d",
                totalUsers, argon2Users, migrationPercentage, bcryptUsers, unknownUsers
            );
        }
    }
}
