package com.example.franchisesapi.dto;

import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.model.BranchStatus;
import com.example.franchisesapi.model.Product;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Producto con más stock de una sucursal, junto con la sucursal a la que pertenece.
 */
@Schema(description = "Producto con más stock de una sucursal, junto con la sucursal a la que pertenece")
public record TopStockProductResponse(
        @Schema(example = "39b98175-891a-4f25-935d-ed5d67f8256b") String branchId,
        @Schema(example = "BOG-001") String branchCode,
        @Schema(example = "Centro") String branchName,
        BranchStatus branchStatus,
        @Schema(example = "7f13092a-0327-4c14-8210-f557660e911d") String productId,
        @Schema(example = "HAM-001") String sku,
        @Schema(example = "Hamburguesa clásica") String productName,
        @Schema(example = "Hamburguesas") String category,
        @Schema(example = "40") int stock,
        @Schema(example = "10") int minimumStock,
        @Schema(description = "true si el stock es igual o inferior al stock mínimo", example = "false")
        boolean lowStock
) {

    public static TopStockProductResponse from(Branch branch, Product product) {
        return new TopStockProductResponse(
                branch.getId(),
                branch.getCode(),
                branch.getName(),
                branch.getStatus(),
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getStock(),
                product.getMinimumStock(),
                product.isLowStock());
    }

}
