package com.example.franchisesapi.dto;

import com.example.franchisesapi.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Producto de una sucursal")
public record ProductResponse(
        @Schema(example = "7f13092a-0327-4c14-8210-f557660e911d") String id,
        @Schema(example = "HAM-001") String sku,
        @Schema(example = "Hamburguesa clásica") String name,
        @Schema(example = "Carne de res, queso cheddar y vegetales") String description,
        @Schema(example = "Hamburguesas") String category,
        @Schema(example = "18900.00") BigDecimal price,
        @Schema(example = "COP") String currency,
        @Schema(example = "40") int stock,
        @Schema(example = "10") int minimumStock,
        @Schema(description = "true si el stock es igual o inferior al stock mínimo", example = "false")
        boolean lowStock,
        @Schema(description = "Si el producto se sigue vendiendo", example = "true") boolean active,
        @Schema(example = "2026-10-27T15:04:05Z") Instant createdAt,
        @Schema(example = "2026-10-27T15:04:05Z") Instant updatedAt
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getCurrency(),
                product.getStock(),
                product.getMinimumStock(),
                product.isLowStock(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }

}
