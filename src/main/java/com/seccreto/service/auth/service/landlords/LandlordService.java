package com.seccreto.service.auth.service.landlords;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.landlords.Landlord;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Abstração da camada de serviço para operações de landlord.
 */
public interface LandlordService {
    
    // Operações básicas CRUD
    Landlord createLandlord(String name, JsonNode config);
    List<Landlord> listAllLandlords();
    Optional<Landlord> findLandlordById(UUID id);
    Optional<Landlord> findLandlordByName(String name);
    Landlord updateLandlord(UUID id, String name, JsonNode config);
    boolean deleteLandlord(UUID id);
    boolean existsLandlordById(UUID id);
    boolean existsLandlordByName(String name);
    long countLandlords();
}