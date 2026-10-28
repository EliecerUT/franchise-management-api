package com.example.franchisesapi.dto;

import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.model.BranchStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Sucursal con sus productos")
public record BranchResponse(
        @Schema(example = "39b98175-891a-4f25-935d-ed5d67f8256b") String id,
        @Schema(example = "BOG-001") String code,
        @Schema(example = "Centro") String name,
        AddressResponse address,
        @Schema(example = "+57 601 555 0001") String phone,
        @Schema(example = "centro@burgerexpress.com") String email,
        @Schema(example = "Laura Gómez") String managerName,
        @Schema(example = "2020-03-15") LocalDate openingDate,
        BranchStatus status,
        List<ProductResponse> products,
        @Schema(example = "2026-10-27T15:04:05Z") Instant createdAt,
        @Schema(example = "2026-10-27T15:04:05Z") Instant updatedAt
) {

    public static BranchResponse from(Branch branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getCode(),
                branch.getName(),
                AddressResponse.from(branch.getAddress()),
                branch.getPhone(),
                branch.getEmail(),
                branch.getManagerName(),
                branch.getOpeningDate(),
                branch.getStatus(),
                branch.getProducts().stream().map(ProductResponse::from).toList(),
                branch.getCreatedAt(),
                branch.getUpdatedAt());
    }

}
