package com.example.franchisesapi.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Producto ofertado en una sucursal, embebido dentro de ella.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product {

    public static final String DEFAULT_CURRENCY = "COP";

    private String id;

    /** Código de producto. Único dentro de la sucursal. */
    private String sku;

    private String name;

    private String description;

    private String category;

    /** BigDecimal para evitar errores de redondeo; se guarda como Decimal128 en MongoDB. */
    private BigDecimal price;

    /** Código de moneda ISO 4217. */
    @Builder.Default
    private String currency = DEFAULT_CURRENCY;

    private int stock;

    /** Umbral de reposición: con este stock o menos el producto se considera en stock bajo. */
    private int minimumStock;

    /** Indica si el producto se sigue vendiendo. */
    @Builder.Default
    private boolean active = true;

    private Instant createdAt;

    private Instant updatedAt;

    public boolean isLowStock() {
        return stock <= minimumStock;
    }

}
