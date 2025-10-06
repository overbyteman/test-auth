package com.seccreto.service.auth.api.mapper.tenants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seccreto.service.auth.api.dto.tenants.TenantRequest;
import com.seccreto.service.auth.api.dto.tenants.TenantResponse;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.tenants.Tenant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class TenantMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void toEntity_and_updateEntity_and_toResponse() {
        ObjectNode config = objectMapper.createObjectNode();
        config.put("key", "value");

        TenantRequest req = TenantRequest.builder()
                .name("Empresa XYZ")
                .config(config)
                .landlordId(UUID.randomUUID())
                .build();

        Tenant entity = TenantMapper.toEntity(req);
        assertNotNull(entity);
        assertEquals("Empresa XYZ", entity.getName());
        assertEquals(config, entity.getConfig());

        Landlord landlord = new Landlord();
        landlord.setId(UUID.randomUUID());
        landlord.setName("Landlord A");
        entity.setLandlord(landlord);

        TenantResponse resp = TenantMapper.toResponse(entity);
        assertNotNull(resp);
        assertEquals(entity.getId(), resp.getId());
        assertEquals(entity.getName(), resp.getName());
        assertEquals(entity.getConfig(), resp.getConfig());
        assertEquals(landlord.getId(), resp.getLandlordId());
        assertEquals(landlord.getName(), resp.getLandlordName());

        // updateEntity
        TenantRequest update = TenantRequest.builder().name("Empresa Z").config(config).landlordId(landlord.getId()).build();
        TenantMapper.updateEntity(entity, update);
        assertEquals("Empresa Z", entity.getName());
    }

    @Test
    void toResponseList_and_nullHandling() {
        assertNull(TenantMapper.toEntity(null));
        assertNull(TenantMapper.toResponse(null));
        assertNull(TenantMapper.toResponseList(null));

        Tenant t1 = new Tenant();
        t1.setId(UUID.randomUUID());
        t1.setName("T1");

        List<TenantResponse> list = TenantMapper.toResponseList(List.of(t1));
        assertEquals(1, list.size());
        assertEquals(t1.getName(), list.get(0).getName());
    }
}
