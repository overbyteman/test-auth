package com.seccreto.service.auth.repository.landlords;

import com.seccreto.service.auth.model.landlords.Landlord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository JPA para Landlord
 */
@Repository
public interface LandlordRepository extends JpaRepository<Landlord, UUID> {
    
    Optional<Landlord> findByName(String name);
    
    List<Landlord> findByNameContainingIgnoreCase(String name);
    
    @Query("SELECT l FROM Landlord l ORDER BY l.createdAt DESC")
    List<Landlord> findAllOrderByCreatedAtDesc();
}