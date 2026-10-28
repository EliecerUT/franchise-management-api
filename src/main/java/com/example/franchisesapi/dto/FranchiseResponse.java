package com.example.franchisesapi.dto;

import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.model.FranchiseStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Franquicia con sus sucursales y productos")
public record FranchiseResponse(
        @Schema(example = "6ae0375cc698e94971e2fce3") String id,
        @Schema(example = "Burger Express") String name,
        @Schema(example = "Burger Express S.A.S.") String legalName,
        @Schema(example = "900123456-7") String taxId,
        @Schema(example = "Cadena de hamburguesas artesanales") String description,
        @Schema(example = "contacto@burgerexpress.com") String email,
        @Schema(example = "+57 601 555 1234") String phone,
        @Schema(example = "https://burgerexpress.com") String website,
        FranchiseStatus status,
        List<BranchResponse> branches,
        @Schema(example = "2026-10-27T15:04:05Z") Instant createdAt,
        @Schema(example = "2026-10-27T15:04:05Z") Instant updatedAt
) {

    public static FranchiseResponse from(Franchise franchise) {
        return new FranchiseResponse(
                franchise.getId(),
                franchise.getName(),
                franchise.getLegalName(),
                franchise.getTaxId(),
                franchise.getDescription(),
                franchise.getEmail(),
                franchise.getPhone(),
                franchise.getWebsite(),
                franchise.getStatus(),
                franchise.getBranches().stream().map(BranchResponse::from).toList(),
                franchise.getCreatedAt(),
                franchise.getUpdatedAt());
    }

}
