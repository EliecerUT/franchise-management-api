package com.example.franchisesapi.dto;

import com.example.franchisesapi.model.BranchStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@Schema(description = "Datos para agregar una sucursal a una franquicia. Solo el nombre es obligatorio.")
public record CreateBranchRequest(

        @Schema(description = "Nombre de la sucursal, único dentro de la franquicia",
                example = "Centro", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank @Size(max = 100)
        String name,

        @Schema(description = "Código interno, único dentro de la franquicia. Se guarda en mayúsculas",
                example = "BOG-001")
        @Pattern(regexp = ValidationPatterns.CODE, message = ValidationPatterns.CODE_MESSAGE)
        String code,

        @Valid
        AddressRequest address,

        @Schema(description = "Teléfono de la sucursal", example = "+57 601 555 0001")
        @Pattern(regexp = ValidationPatterns.PHONE, message = ValidationPatterns.PHONE_MESSAGE)
        String phone,

        @Schema(description = "Correo de la sucursal", example = "centro@burgerexpress.com")
        @Email @Size(max = 150)
        String email,

        @Schema(description = "Responsable de la sucursal", example = "Laura Gómez")
        @Size(max = 100)
        String managerName,

        @Schema(description = "Fecha de apertura; no puede ser futura", example = "2020-03-15")
        @PastOrPresent
        LocalDate openingDate,

        @Schema(description = "Estado inicial; OPEN si no se indica", example = "OPEN")
        BranchStatus status
) {
}
