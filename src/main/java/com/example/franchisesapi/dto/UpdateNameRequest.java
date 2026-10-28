package com.example.franchisesapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nuevo nombre del recurso")
public record UpdateNameRequest(

        @Schema(description = "Nuevo nombre, único en su ámbito sin distinguir mayúsculas",
                example = "Burger Express Gourmet", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String name
) {
}
