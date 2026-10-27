package com.example.franchisesapi.exception;

/**
 * La petición es válida pero el estado actual de los datos no permite la operación
 * (p. ej. agregar productos a una sucursal cerrada). Se traduce a 422.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }

}
