package com.example.franchisesapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "Nuevo stock del producto")
public record UpdateStockRequest(

        @Schema(description = "Unidades disponibles", example = "15", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @PositiveOrZero
        Integer stock
) {
}
