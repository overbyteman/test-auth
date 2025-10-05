package com.seccreto.service.auth.model.landlords;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;
import com.seccreto.service.auth.model.roles.Role;
import com.seccreto.service.auth.model.tenants.Tenant;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Landlord - Entidade que representa o proprietário de um ou mais tenants (filiais).
 */
@Entity
@Table(name = "landlords",
       indexes = {
           @Index(name = "idx_landlords_name", columnList = "name")
       })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@Schema(description = "Landlord responsável por múltiplos tenants")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"tenants", "roles"})
public class Landlord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 200)
    private String name;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private JsonNode config;

    @OneToMany(mappedBy = "landlord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Tenant> tenants = new HashSet<>();

    @OneToMany(mappedBy = "landlord", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    public static Landlord createNew(String name, JsonNode config) {
        return Landlord.builder()
                .name(name)
                .config(config)
        .tenants(new HashSet<>())
        .roles(new HashSet<>())
                .build();
    }

    public void addTenant(Tenant tenant) {
        if (tenant == null) {
            return;
        }
        tenants.add(tenant);
        tenant.setLandlord(this);
    }

    public void removeTenant(Tenant tenant) {
        if (tenant == null) {
            return;
        }
        tenants.remove(tenant);
        tenant.setLandlord(null);
    }

    public void addRole(Role role) {
        if (role == null) {
            return;
        }
        roles.add(role);
        role.setLandlord(this);
    }

    public void removeRole(Role role) {
        if (role == null) {
            return;
        }
        roles.remove(role);
        if (role.getLandlord() != null && role.getLandlord().equals(this)) {
            role.setLandlord(null);
        }
    }
}
