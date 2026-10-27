package com.example.franchisesapi.dto;

import com.example.franchisesapi.model.Address;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dirección de una sucursal")
public record AddressResponse(
        @Schema(example = "Cra 7 # 12-34") String street,
        @Schema(example = "Bogotá") String city,
        @Schema(example = "Cundinamarca") String state,
        @Schema(example = "Colombia") String country,
        @Schema(example = "110311") String postalCode
) {

    /** Devuelve null si la sucursal no tiene dirección registrada. */
    public static AddressResponse from(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getStreet(),
                address.getCity(),
                address.getState(),
                address.getCountry(),
                address.getPostalCode());
    }

}
