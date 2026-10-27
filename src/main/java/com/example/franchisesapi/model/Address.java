package com.example.franchisesapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dirección física de una sucursal, embebida dentro de ella.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    private String street;

    private String city;

    /** Departamento, estado o provincia. */
    private String state;

    private String country;

    private String postalCode;

}
