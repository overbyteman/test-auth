package com.seccreto.service.auth.api.mapper.roles;

import com.seccreto.service.auth.api.dto.roles.RoleRequest;
import com.seccreto.service.auth.api.dto.roles.RoleResponse;
import com.seccreto.service.auth.model.landlords.Landlord;
import com.seccreto.service.auth.model.roles.Role;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RoleMapperTest {

    @Test
    void toEntity_toResponse_and_update() {
        RoleRequest req = RoleRequest.builder().code("ADMIN").name("Admin").description("Administrator").build();

        Role role = RoleMapper.toEntity(req);
        assertNotNull(role);
        assertEquals("ADMIN", role.getCode());
        assertEquals("Admin", role.getName());

        Landlord landlord = new Landlord();
        landlord.setId(UUID.randomUUID());
        landlord.setName("Landlord 1");
        role.setLandlord(landlord);
        role.setId(UUID.randomUUID());

        RoleResponse resp = RoleMapper.toResponse(role);
        assertNotNull(resp);
        assertEquals(role.getId(), resp.getId());
        assertEquals(role.getCode(), resp.getCode());
        assertEquals(landlord.getId(), resp.getLandlordId());

        // update
        RoleRequest update = RoleRequest.builder().code("USER").name("User").description("User desc").build();
        RoleMapper.updateEntity(role, update);
        assertEquals("USER", role.getCode());
        assertEquals("User", role.getName());
    }

    @Test
    void list_and_nullHandling() {
        assertNull(RoleMapper.toEntity(null));
        assertNull(RoleMapper.toResponse(null));
        assertNull(RoleMapper.toResponseList(null));

        Role r = new Role();
        r.setId(UUID.randomUUID());
        r.setName("R1");

        List<RoleResponse> list = RoleMapper.toResponseList(List.of(r));
        assertEquals(1, list.size());
        assertEquals(r.getName(), list.get(0).getName());
    }
}
