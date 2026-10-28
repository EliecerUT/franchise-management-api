package com.example.franchisesapi.model;

/**
 * Estado comercial de una franquicia.
 */
public enum FranchiseStatus {

    /** Opera con normalidad: admite nuevas sucursales. */
    ACTIVE,

    /** Dada de baja: se conserva su información pero no admite nuevas sucursales. */
    INACTIVE

}
