package com.seccreto.service.auth.service.landlords;

import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.repository.landlords.LandlordRepository;
import com.seccreto.service.auth.service.exception.ConflictException;
import com.seccreto.service.auth.service.exception.ResourceNotFoundException;
import com.seccreto.service.auth.service.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação da camada de serviço para operações de landlord.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LandlordServiceImpl implements LandlordService {

    private final LandlordRepository landlordRepository;

    @Override
    @Transactional
    public Landlord createLandlord(String name, JsonNode config) {
        validateName(name);
        
        if (existsLandlordByName(name)) {
            throw new ConflictException("Landlord com nome '" + name + "' já existe");
        }

        Landlord landlord = Landlord.createNew(name, config);
        return landlordRepository.save(landlord);
    }

    @Override
    public List<Landlord> listAllLandlords() {
        return landlordRepository.findAll();
    }

    @Override
    public Optional<Landlord> findLandlordById(UUID id) {
        validateId(id);
        return landlordRepository.findById(id);
    }

    @Override
    public Optional<Landlord> findLandlordByName(String name) {
        validateName(name);
        return landlordRepository.findByName(name);
    }

    @Override
    @Transactional
    public Landlord updateLandlord(UUID id, String name, JsonNode config) {
        validateId(id);
        validateName(name);
        
        Landlord landlord = findLandlordById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Landlord não encontrado: " + id));

        // Verificar se o novo nome já existe em outro landlord
        Optional<Landlord> existingByName = landlordRepository.findByName(name);
        if (existingByName.isPresent() && !existingByName.get().getId().equals(id)) {
            throw new ConflictException("Nome '" + name + "' já está em uso por outro landlord");
        }

        landlord.setName(name);
        landlord.setConfig(config);
        
        return landlordRepository.save(landlord);
    }

    @Override
    @Transactional
    public boolean deleteLandlord(UUID id) {
        validateId(id);
        
        if (!existsLandlordById(id)) {
            return false;
        }
        
        landlordRepository.deleteById(id);
        return true;
    }

    @Override
    public boolean existsLandlordById(UUID id) {
        validateId(id);
        return landlordRepository.existsById(id);
    }

    @Override
    public boolean existsLandlordByName(String name) {
        validateName(name);
        return landlordRepository.findByName(name).isPresent();
    }

    @Override
    public long countLandlords() {
        return landlordRepository.count();
    }

    // ===== MÉTODOS PRIVADOS =====

    private void validateId(UUID id) {
        if (id == null) {
            throw new ValidationException("ID do landlord é obrigatório");
        }
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Nome do landlord é obrigatório");
        }
        if (name.length() > 200) {
            throw new ValidationException("Nome do landlord deve ter no máximo 200 caracteres");
        }
    }
}