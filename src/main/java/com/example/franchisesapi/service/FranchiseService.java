package com.example.franchisesapi.service;

import com.example.franchisesapi.dto.CreateFranchiseRequest;
import com.example.franchisesapi.dto.TopStockProductResponse;
import com.example.franchisesapi.exception.DuplicateResourceException;
import com.example.franchisesapi.exception.ResourceNotFoundException;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.repository.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.example.franchisesapi.service.InputNormalizer.lower;
import static com.example.franchisesapi.service.InputNormalizer.text;
import static com.example.franchisesapi.service.InputNormalizer.upper;

@Service
@RequiredArgsConstructor
public class FranchiseService {

    private final FranchiseRepository franchiseRepository;

    /**
     * Crea la franquicia en estado ACTIVE y sin sucursales. Las fechas de creación y
     * modificación las asigna la auditoría de Spring Data al guardar.
     */
    public Franchise create(CreateFranchiseRequest request) {
        String name = text(request.name());
        String taxId = upper(request.taxId());
        if (franchiseRepository.existsByNameIgnoreCase(name)) {
            throw new DuplicateResourceException("Franchise '%s' already exists".formatted(name));
        }
        if (taxId != null && franchiseRepository.existsByTaxId(taxId)) {
            throw new DuplicateResourceException("A franchise with tax id '%s' already exists".formatted(taxId));
        }

        Franchise franchise = Franchise.builder()
                .name(name)
                .legalName(text(request.legalName()))
                .taxId(taxId)
                .description(text(request.description()))
                .email(lower(request.email()))
                .phone(text(request.phone()))
                .website(text(request.website()))
                .build();
        return franchiseRepository.save(franchise);
    }

    public List<Franchise> findAll() {
        return franchiseRepository.findAll();
    }

    /**
     * Punto de entrada común para el resto de servicios: toda operación sobre sucursales
     * o productos empieza cargando su franquicia.
     */
    public Franchise getById(String franchiseId) {
        return franchiseRepository.findById(franchiseId)
                .orElseThrow(() -> ResourceNotFoundException.franchise(franchiseId));
    }

    /**
     * Cambia el nombre comercial. Se permite cambiar solo las mayúsculas del propio nombre,
     * pero no usar el de otra franquicia.
     */
    public Franchise updateName(String franchiseId, String newName) {
        Franchise franchise = getById(franchiseId);
        String name = text(newName);
        if (franchiseRepository.existsByNameIgnoreCaseAndIdNot(name, franchiseId)) {
            throw new DuplicateResourceException("Franchise '%s' already exists".formatted(name));
        }

        franchise.setName(name);
        return franchiseRepository.save(franchise);
    }

    /**
     * Producto con más stock de cada sucursal de la franquicia, en el orden de las sucursales.
     * Las sucursales sin productos no aparecen en el resultado.
     */
    public List<TopStockProductResponse> getTopStockProducts(String franchiseId) {
        return getById(franchiseId).getBranches().stream()
                .flatMap(branch -> branch.findTopStockProduct()
                        .map(product -> TopStockProductResponse.from(branch, product))
                        .stream())
                .toList();
    }

}
