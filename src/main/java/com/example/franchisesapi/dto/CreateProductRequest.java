package com.example.franchisesapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Datos para agregar un producto a una sucursal. Nombre, precio y stock son obligatorios.")
public record CreateProductRequest(

        @Schema(description = "Nombre del producto, único dentro de la sucursal",
                example = "Hamburguesa clásica", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String name,

        @Schema(description = "Código de producto, único dentro de la sucursal. Se guarda en mayúsculas",
                example = "HAM-001")
        @Pattern(regexp = ValidationPatterns.CODE, message = ValidationPatterns.CODE_MESSAGE)
        String sku,

        @Schema(example = "Carne de res, queso cheddar y vegetales")
        @Size(max = 500)
        String description,

        @Schema(example = "Hamburguesas")
        @Size(max = 60)
        String category,

        @Schema(description = "Precio unitario, mayor que cero y con hasta 2 decimales",
                example = "18900.00", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 12, fraction = 2)
        BigDecimal price,

        @Schema(description = "Moneda ISO 4217; COP si no se indica", example = "COP")
        @Pattern(regexp = ValidationPatterns.CURRENCY, message = ValidationPatterns.CURRENCY_MESSAGE)
        String currency,

        @Schema(description = "Unidades disponibles", example = "40", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @PositiveOrZero
        Integer stock,

        @Schema(description = "Umbral de reposición; 0 si no se indica", example = "10")
        @PositiveOrZero
        Integer minimumStock,

        @Schema(description = "Si el producto se vende; true si no se indica", example = "true")
        Boolean active
) {
}
