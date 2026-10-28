package com.example.franchisesapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dirección de la sucursal. Ciudad y país son obligatorios si se envía la dirección.")
public record AddressRequest(

        @Schema(description = "Calle y número", example = "Cra 7 # 12-34")
        @Size(max = 150)
        String street,

        @Schema(example = "Bogotá", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String city,

        @Schema(description = "Departamento, estado o provincia", example = "Cundinamarca")
        @Size(max = 100)
        String state,

        @Schema(example = "Colombia", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String country,

        @Schema(example = "110311")
        @Size(max = 20)
        String postalCode
) {
}
