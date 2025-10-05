package com.seccreto.service.auth.api.mapper.landlords;

import com.seccreto.service.auth.api.dto.landlords.LandlordResponse;
import com.seccreto.service.auth.model.landlords.Landlord;

/**
 * Mapper para conversão entre entidades Landlord e DTOs
 */
public class LandlordMapper {
    
    public static LandlordResponse toResponse(Landlord landlord) {
        if (landlord == null) {
            return null;
        }
        
        return LandlordResponse.builder()
            .id(landlord.getId())
            .name(landlord.getName())
            .config(landlord.getConfig())
            .createdAt(landlord.getCreatedAt())
            .updatedAt(landlord.getUpdatedAt())
            .tenantsCount(landlord.getTenants() != null ? landlord.getTenants().size() : 0)
            .rolesCount(landlord.getRoles() != null ? landlord.getRoles().size() : 0)
            .build();
    }
}