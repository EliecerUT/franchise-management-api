package com.example.franchisesapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Raíz del agregado: una franquicia se guarda como un único documento en MongoDB
 * que contiene sus sucursales y, dentro de cada una, sus productos.
 */
@Document(collection = "franchises")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Franchise {

    @Id
    private String id;

    /** Nombre comercial. Único sin distinguir mayúsculas (collation con strength 2). */
    @Indexed(unique = true, collation = "{ 'locale': 'es', 'strength': 2 }")
    private String name;

    /** Razón social. */
    private String legalName;

    /** Identificación fiscal (NIT, RUT, ...). Única cuando se informa. */
    @Indexed(unique = true, sparse = true)
    private String taxId;

    private String description;

    private String email;

    private String phone;

    private String website;

    @Builder.Default
    private FranchiseStatus status = FranchiseStatus.ACTIVE;

    @Builder.Default
    private List<Branch> branches = new ArrayList<>();

    @CreatedDate
    private Instant createdAt;

    /** Se actualiza en cualquier cambio del agregado, incluidos los de sus sucursales y productos. */
    @LastModifiedDate
    private Instant updatedAt;

    /**
     * Control de concurrencia optimista: si dos peticiones modifican la misma franquicia
     * a la vez, la segunda en guardar falla en lugar de sobrescribir los cambios de la primera.
     */
    @Version
    private Long version;

    public boolean isActive() {
        return status == FranchiseStatus.ACTIVE;
    }

    public Optional<Branch> findBranch(String branchId) {
        return branches.stream()
                .filter(branch -> branch.getId().equals(branchId))
                .findFirst();
    }

    public boolean hasBranchNamed(String branchName) {
        return branches.stream()
                .anyMatch(branch -> branch.getName().equalsIgnoreCase(branchName));
    }

    /** Para renombrar: busca el nombre en las demás sucursales, sin contar la que se renombra. */
    public boolean hasOtherBranchNamed(String branchId, String branchName) {
        return branches.stream()
                .anyMatch(branch -> !branch.getId().equals(branchId) && branch.getName().equalsIgnoreCase(branchName));
    }

    public boolean hasBranchWithCode(String branchCode) {
        return branchCode != null && branches.stream()
                .anyMatch(branch -> branchCode.equalsIgnoreCase(branch.getCode()));
    }

}
