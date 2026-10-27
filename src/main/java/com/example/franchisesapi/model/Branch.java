package com.example.franchisesapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Sucursal embebida dentro del documento de su franquicia.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Branch {

    private String id;

    /** Código interno de la sucursal (p. ej. BOG-001). Único dentro de la franquicia. */
    private String code;

    private String name;

    private Address address;

    private String phone;

    private String email;

    private String managerName;

    private LocalDate openingDate;

    @Builder.Default
    private BranchStatus status = BranchStatus.OPEN;

    @Builder.Default
    private List<Product> products = new ArrayList<>();

    /** La auditoría de Spring Data solo cubre el documento raíz: estas fechas las asigna el servicio. */
    private Instant createdAt;

    private Instant updatedAt;

    public boolean isClosed() {
        return status == BranchStatus.CLOSED;
    }

    public Optional<Product> findProduct(String productId) {
        return products.stream()
                .filter(product -> product.getId().equals(productId))
                .findFirst();
    }

    public boolean hasProductNamed(String productName) {
        return products.stream()
                .anyMatch(product -> product.getName().equalsIgnoreCase(productName));
    }

    public boolean hasProductWithSku(String sku) {
        return sku != null && products.stream()
                .anyMatch(product -> sku.equalsIgnoreCase(product.getSku()));
    }

    public boolean removeProduct(String productId) {
        return products.removeIf(product -> product.getId().equals(productId));
    }

    /**
     * Producto con más stock de la sucursal; vacío si no tiene productos.
     * En caso de empate gana el que se agregó primero.
     */
    public Optional<Product> findTopStockProduct() {
        return products.stream()
                .max(Comparator.comparingInt(Product::getStock));
    }

}
