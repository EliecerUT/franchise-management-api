package com.example.franchisesapi.exception;

/**
 * Ya existe un recurso con el mismo nombre en el mismo ámbito. Se traduce a 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }

}
