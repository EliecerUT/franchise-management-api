package com.example.franchisesapi.model;

/**
 * Estado operativo de una sucursal.
 */
public enum BranchStatus {

    /** Abierta al público. */
    OPEN,

    /** Cerrada de forma temporal (reformas, vacaciones, ...). */
    TEMPORARILY_CLOSED,

    /** Cerrada de forma definitiva: no admite nuevos productos. */
    CLOSED

}
