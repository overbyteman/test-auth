package com.seccreto.service.auth.repository.policies;

import com.seccreto.service.auth.model.policies.Policy;
import com.seccreto.service.auth.model.policies.PolicyEffect;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA para Policy - Substitui o JdbcPolicyRepository
 */
@Repository
public interface PolicyRepository extends JpaRepository<Policy, UUID> {

    // ========================================
    // QUERIES DERIVADAS AUTOMÁTICAS (JPA)
    // ========================================
    
    Optional<Policy> findByName(String name);
    
    List<Policy> findByEffect(PolicyEffect effect);
    
    List<Policy> findByNameContainingIgnoreCase(String name);
    
    List<Policy> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    boolean existsByName(String name);

    // ========================================
    // MÉTODOS PARA COMPATIBILIDADE COM SERVICES
    // ========================================
    
    @Query("SELECT p FROM Policy p WHERE LOWER(p.name) = LOWER(:name)")
    Optional<Policy> findByNameExact(@Param("name") String name);

    // ========================================
    // QUERIES CUSTOMIZADAS COM @Query
    // ========================================
    
    @Query("SELECT p FROM Policy p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Policy> search(@Param("query") String query);
    
    @Query("SELECT p FROM Policy p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "OR LOWER(p.description) LIKE LOWER(CONCAT('%', :query, '%'))")
    Page<Policy> search(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT p FROM Policy p WHERE p.createdAt >= :since")
    List<Policy> findRecentPolicies(@Param("since") LocalDateTime since);

    // ========================================
    // QUERIES PARA EFEITOS E ESTATÍSTICAS
    // ========================================
    
    @Query("SELECT p.effect, COUNT(p) FROM Policy p GROUP BY p.effect")
    List<Object[]> getPolicyEffectDistribution();
    
    @Query("SELECT COUNT(p) FROM Policy p WHERE p.effect = :effect")
    long countByEffect(@Param("effect") PolicyEffect effect);
    
    @Query("SELECT COUNT(p) FROM Policy p WHERE p.createdAt >= :since")
    long countPoliciesCreatedSince(@Param("since") LocalDateTime since);

    // ========================================
    // QUERIES NATIVAS PARA JSON (PostgreSQL/H2)
    // ========================================
    
    /**
     * Busca policies que contêm uma ação específica no array JSON
     */
    @Query(value = "SELECT * FROM policies WHERE JSON_SEARCH(actions, 'one', :action) IS NOT NULL", 
           nativeQuery = true)
    List<Policy> findByAction(@Param("action") String action);
    
    /**
     * Busca policies que contêm um recurso específico no array JSON
     */
    @Query(value = "SELECT * FROM policies WHERE JSON_SEARCH(resources, 'one', :resource) IS NOT NULL", 
           nativeQuery = true)
    List<Policy> findByResource(@Param("resource") String resource);
    
    /**
     * Busca policies que contêm tanto a ação quanto o recurso
     */
    @Query(value = "SELECT * FROM policies WHERE " +
                   "JSON_SEARCH(actions, 'one', :action) IS NOT NULL AND " +
                   "JSON_SEARCH(resources, 'one', :resource) IS NOT NULL", 
           nativeQuery = true)
    List<Policy> findByActionAndResource(@Param("action") String action, @Param("resource") String resource);

    // ========================================
    // MÉTODOS ADICIONAIS PARA SERVICES
    // ========================================
    
    @Query("SELECT p FROM Policy p WHERE p.effect = :effect AND CAST(p.conditions AS string) LIKE %:conditions%")
    List<Policy> findByEffectAndConditions(@Param("effect") PolicyEffect effect, @Param("conditions") String conditions);
}