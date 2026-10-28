package com.example.franchisesapi.controller;

import com.example.franchisesapi.config.OpenApiConfig;
import com.example.franchisesapi.dto.CreateFranchiseRequest;
import com.example.franchisesapi.dto.FranchiseResponse;
import com.example.franchisesapi.dto.TopStockProductResponse;
import com.example.franchisesapi.dto.UpdateNameRequest;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.service.FranchiseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/franchises")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.FRANCHISES_TAG)
public class FranchiseController {

    private final FranchiseService franchiseService;

    @PostMapping
    @Operation(summary = "Agregar una nueva franquicia",
            description = "Crea la franquicia en estado ACTIVE y sin sucursales. El nombre y el NIT deben ser únicos.")
    @ApiResponse(responseCode = "201", description = "Franquicia creada",
            headers = @Header(name = "Location", description = "URL de la franquicia creada",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "400", ref = OpenApiConfig.BAD_REQUEST_RESPONSE)
    @ApiResponse(responseCode = "409", ref = OpenApiConfig.CONFLICT_RESPONSE)
    public ResponseEntity<FranchiseResponse> create(@Valid @RequestBody CreateFranchiseRequest request) {
        Franchise franchise = franchiseService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{franchiseId}")
                .buildAndExpand(franchise.getId())
                .toUri();
        return ResponseEntity.created(location).body(FranchiseResponse.from(franchise));
    }

    @GetMapping
    @Operation(summary = "Listar todas las franquicias")
    @ApiResponse(responseCode = "200", description = "Franquicias con sus sucursales y productos")
    public List<FranchiseResponse> findAll() {
        return franchiseService.findAll().stream().map(FranchiseResponse::from).toList();
    }

    @GetMapping("/{franchiseId}")
    @Operation(summary = "Consultar una franquicia con sus sucursales y productos")
    @ApiResponse(responseCode = "200", description = "Franquicia encontrada")
    @ApiResponse(responseCode = "404", ref = OpenApiConfig.NOT_FOUND_RESPONSE)
    public FranchiseResponse getById(
            @Parameter(description = "Id de la franquicia", example = "6ae0375cc698e94971e2fce3")
            @PathVariable String franchiseId) {
        return FranchiseResponse.from(franchiseService.getById(franchiseId));
    }

    @PatchMapping("/{franchiseId}/name")
    @Operation(summary = "Modificar el nombre de una franquicia",
            description = "El nuevo nombre debe ser único en toda la API, sin distinguir mayúsculas.")
    @ApiResponse(responseCode = "200", description = "Nombre actualizado")
    @ApiResponse(responseCode = "400", ref = OpenApiConfig.BAD_REQUEST_RESPONSE)
    @ApiResponse(responseCode = "404", ref = OpenApiConfig.NOT_FOUND_RESPONSE)
    @ApiResponse(responseCode = "409", ref = OpenApiConfig.CONFLICT_RESPONSE)
    public FranchiseResponse updateName(
            @Parameter(description = "Id de la franquicia", example = "6ae0375cc698e94971e2fce3")
            @PathVariable String franchiseId,
            @Valid @RequestBody UpdateNameRequest request) {
        return FranchiseResponse.from(franchiseService.updateName(franchiseId, request.name()));
    }

    @GetMapping("/{franchiseId}/top-stock-products")
    @Operation(summary = "Producto con más stock de cada sucursal de una franquicia",
            description = "Devuelve un elemento por sucursal con el producto de mayor stock y la sucursal a la que "
                    + "pertenece. Las sucursales sin productos no aparecen; en caso de empate gana el producto "
                    + "agregado primero.")
    @ApiResponse(responseCode = "200", description = "Reporte de stock por sucursal")
    @ApiResponse(responseCode = "404", ref = OpenApiConfig.NOT_FOUND_RESPONSE)
    public List<TopStockProductResponse> getTopStockProducts(
            @Parameter(description = "Id de la franquicia", example = "6ae0375cc698e94971e2fce3")
            @PathVariable String franchiseId) {
        return franchiseService.getTopStockProducts(franchiseId);
    }

}
