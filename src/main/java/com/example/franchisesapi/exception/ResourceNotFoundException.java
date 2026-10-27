package com.example.franchisesapi.exception;

/**
 * El recurso pedido (franquicia, sucursal o producto) no existe. Se traduce a 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException franchise(String franchiseId) {
        return new ResourceNotFoundException("Franchise '%s' not found".formatted(franchiseId));
    }

    public static ResourceNotFoundException branch(String branchId) {
        return new ResourceNotFoundException("Branch '%s' not found".formatted(branchId));
    }

    public static ResourceNotFoundException product(String productId) {
        return new ResourceNotFoundException("Product '%s' not found".formatted(productId));
    }

}
