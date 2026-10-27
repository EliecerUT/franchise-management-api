package com.example.franchisesapi.controller;

import com.example.franchisesapi.config.OpenApiConfig;
import com.example.franchisesapi.dto.CreateProductRequest;
import com.example.franchisesapi.dto.ProductResponse;
import com.example.franchisesapi.dto.UpdateStockRequest;
import com.example.franchisesapi.model.Product;
import com.example.franchisesapi.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/franchises/{franchiseId}/branches/{branchId}/products")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.PRODUCTS_TAG)
public class ProductController {

    private static final String FRANCHISE_ID_EXAMPLE = "6ae0375cc698e94971e2fce3";
    private static final String BRANCH_ID_EXAMPLE = "39b98175-891a-4f25-935d-ed5d67f8256b";
    private static final String PRODUCT_ID_EXAMPLE = "7f13092a-0327-4c14-8210-f557660e911d";

    private final ProductService productService;

    @PostMapping
    @Operation(summary = "Agregar un nuevo producto a una sucursal",
            description = "La sucursal no puede estar CLOSED. El nombre y el SKU deben ser únicos dentro de la sucursal.")
    @ApiResponse(responseCode = "201", description = "Producto creado",
            headers = @Header(name = "Location", description = "URL del producto creado",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "400", ref = OpenApiConfig.BAD_REQUEST_RESPONSE)
    @ApiResponse(responseCode = "404", ref = OpenApiConfig.NOT_FOUND_RESPONSE)
    @ApiResponse(responseCode = "409", ref = OpenApiConfig.CONFLICT_RESPONSE)
    @ApiResponse(responseCode = "422", ref = OpenApiConfig.UNPROCESSABLE_RESPONSE)
    public ResponseEntity<ProductResponse> addProduct(
            @Parameter(description = "Id de la franquicia", example = FRANCHISE_ID_EXAMPLE)
            @PathVariable String franchiseId,
            @Parameter(description = "Id de la sucursal", example = BRANCH_ID_EXAMPLE)
            @PathVariable String branchId,
            @Valid @RequestBody CreateProductRequest request) {
        Product product = productService.addProduct(franchiseId, branchId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{productId}")
                .buildAndExpand(product.getId())
                .toUri();
        return ResponseEntity.created(location).body(ProductResponse.from(product));
    }

    @GetMapping("/{productId}")
    @Operation(summary = "Consultar un producto")
    @ApiResponse(responseCode = "200", description = "Producto encontrado")
    @ApiResponse(responseCode = "404", ref = OpenApiConfig.NOT_FOUND_RESPONSE)
    public ProductResponse getById(
            @Parameter(description = "Id de la franquicia", example = FRANCHISE_ID_EXAMPLE)
            @PathVariable String franchiseId,
            @Parameter(description = "Id de la sucursal", example = BRANCH_ID_EXAMPLE)
            @PathVariable String branchId,
            @Parameter(description = "Id del producto", example = PRODUCT_ID_EXAMPLE)
            @PathVariable String productId) {
        return ProductResponse.from(productService.getById(franchiseId, branchId, productId));
    }

    @DeleteMapping("/{productId}")
    @Operation(summary = "Eliminar un producto de una sucursal")
    @ApiResponse(responseCode = "204", description = "Producto eliminado")
    @ApiResponse(responseCode = "404", ref = OpenApiConfig.NOT_FOUND_RESPONSE)
    @ApiResponse(responseCode = "409", ref = OpenApiConfig.CONFLICT_RESPONSE)
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Id de la franquicia", example = FRANCHISE_ID_EXAMPLE)
            @PathVariable String franchiseId,
            @Parameter(description = "Id de la sucursal", example = BRANCH_ID_EXAMPLE)
            @PathVariable String branchId,
            @Parameter(description = "Id del producto", example = PRODUCT_ID_EXAMPLE)
            @PathVariable String productId) {
        productService.deleteProduct(franchiseId, branchId, productId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{productId}/stock")
    @Operation(summary = "Modificar el stock de un producto",
            description = "Reemplaza el stock disponible. La respuesta indica si el producto quedó en stock bajo.")
    @ApiResponse(responseCode = "200", description = "Stock actualizado")
    @ApiResponse(responseCode = "400", ref = OpenApiConfig.BAD_REQUEST_RESPONSE)
    @ApiResponse(responseCode = "404", ref = OpenApiConfig.NOT_FOUND_RESPONSE)
    @ApiResponse(responseCode = "409", ref = OpenApiConfig.CONFLICT_RESPONSE)
    public ProductResponse updateStock(
            @Parameter(description = "Id de la franquicia", example = FRANCHISE_ID_EXAMPLE)
            @PathVariable String franchiseId,
            @Parameter(description = "Id de la sucursal", example = BRANCH_ID_EXAMPLE)
            @PathVariable String branchId,
            @Parameter(description = "Id del producto", example = PRODUCT_ID_EXAMPLE)
            @PathVariable String productId,
            @Valid @RequestBody UpdateStockRequest request) {
        return ProductResponse.from(productService.updateStock(franchiseId, branchId, productId, request.stock()));
    }

}
