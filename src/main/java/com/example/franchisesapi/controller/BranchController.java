package com.example.franchisesapi.controller;

import com.example.franchisesapi.config.OpenApiConfig;
import com.example.franchisesapi.dto.BranchResponse;
import com.example.franchisesapi.dto.CreateBranchRequest;
import com.example.franchisesapi.dto.UpdateNameRequest;
import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.service.BranchService;
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

@RestController
@RequestMapping("/api/v1/franchises/{franchiseId}/branches")
@RequiredArgsConstructor
@Tag(name = OpenApiConfig.BRANCHES_TAG)
public class BranchController {

    private final BranchService branchService;

    @PostMapping
    @Operation(summary = "Agregar una nueva sucursal a una franquicia",
            description = "La franquicia debe estar ACTIVE. El nombre y el código deben ser únicos dentro de la franquicia.")
    @ApiResponse(responseCode = "201", description = "Sucursal creada",
            headers = @Header(name = "Location", description = "URL de la sucursal creada",
                    schema = @Schema(type = "string", format = "uri")))
    @ApiResponse(responseCode = "400", ref = OpenApiConfig.BAD_REQUEST_RESPONSE)
    @ApiResponse(responseCode = "404", ref = OpenApiConfig.NOT_FOUND_RESPONSE)
    @ApiResponse(responseCode = "409", ref = OpenApiConfig.CONFLICT_RESPONSE)
    @ApiResponse(responseCode = "422", ref = OpenApiConfig.UNPROCESSABLE_RESPONSE)
    public ResponseEntity<BranchResponse> addBranch(
            @Parameter(description = "Id de la franquicia", example = "6ae0375cc698e94971e2fce3")
            @PathVariable String franchiseId,
            @Valid @RequestBody CreateBranchRequest request) {
        Branch branch = branchService.addBranch(franchiseId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{branchId}")
                .buildAndExpand(branch.getId())
                .toUri();
        return ResponseEntity.created(location).body(BranchResponse.from(branch));
    }

    @GetMapping("/{branchId}")
    @Operation(summary = "Consultar una sucursal con sus productos")
    @ApiResponse(responseCode = "200", description = "Sucursal encontrada")
    @ApiResponse(responseCode = "404", ref = OpenApiConfig.NOT_FOUND_RESPONSE)
    public BranchResponse getById(
            @Parameter(description = "Id de la franquicia", example = "6ae0375cc698e94971e2fce3")
            @PathVariable String franchiseId,
            @Parameter(description = "Id de la sucursal", example = "39b98175-891a-4f25-935d-ed5d67f8256b")
            @PathVariable String branchId) {
        return BranchResponse.from(branchService.getById(franchiseId, branchId));
    }

    @PatchMapping("/{branchId}/name")
    @Operation(summary = "Modificar el nombre de una sucursal",
            description = "El nuevo nombre debe ser único dentro de la franquicia, sin distinguir mayúsculas.")
    @ApiResponse(responseCode = "200", description = "Nombre actualizado")
    @ApiResponse(responseCode = "400", ref = OpenApiConfig.BAD_REQUEST_RESPONSE)
    @ApiResponse(responseCode = "404", ref = OpenApiConfig.NOT_FOUND_RESPONSE)
    @ApiResponse(responseCode = "409", ref = OpenApiConfig.CONFLICT_RESPONSE)
    public BranchResponse updateName(
            @Parameter(description = "Id de la franquicia", example = "6ae0375cc698e94971e2fce3")
            @PathVariable String franchiseId,
            @Parameter(description = "Id de la sucursal", example = "39b98175-891a-4f25-935d-ed5d67f8256b")
            @PathVariable String branchId,
            @Valid @RequestBody UpdateNameRequest request) {
        return BranchResponse.from(branchService.updateName(franchiseId, branchId, request.name()));
    }

}
