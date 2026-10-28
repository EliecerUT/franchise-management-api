package com.example.franchisesapi.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear una franquicia. Solo el nombre es obligatorio.")
public record CreateFranchiseRequest(

        @Schema(description = "Nombre comercial, único en toda la API sin distinguir mayúsculas",
                example = "Burger Express", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String name,

        @Schema(description = "Razón social", example = "Burger Express S.A.S.")
        @Size(max = 150)
        String legalName,

        @Schema(description = "Identificación fiscal (NIT, RUT, ...). Única si se informa", example = "900123456-7")
        @Pattern(regexp = ValidationPatterns.TAX_ID, message = ValidationPatterns.TAX_ID_MESSAGE)
        String taxId,

        @Schema(description = "Descripción del negocio", example = "Cadena de hamburguesas artesanales")
        @Size(max = 500)
        String description,

        @Schema(description = "Correo de contacto", example = "contacto@burgerexpress.com")
        @Email @Size(max = 150)
        String email,

        @Schema(description = "Teléfono de contacto", example = "+57 601 555 1234")
        @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
        String phone,

        @Schema(description = "Sitio web", example = "https://burgerexpress.com")
        @Pattern(regexp = ValidationPatterns.WEBSITE, message = ValidationPatterns.WEBSITE_MESSAGE)
        @Size(max = 255)
        String website
) {
}
