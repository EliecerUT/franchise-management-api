package com.example.franchisesapi.service;

import com.example.franchisesapi.dto.CreateProductRequest;
import com.example.franchisesapi.exception.BusinessRuleException;
import com.example.franchisesapi.exception.DuplicateResourceException;
import com.example.franchisesapi.exception.ResourceNotFoundException;
import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.model.Product;
import com.example.franchisesapi.repository.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static com.example.franchisesapi.service.InputNormalizer.text;
import static com.example.franchisesapi.service.InputNormalizer.upper;

/**
 * Todas las operaciones siguen el mismo patrón: cargar la franquicia, localizar la sucursal
 * (y el producto), modificar el documento en memoria y guardarlo completo.
 */
@Service
@RequiredArgsConstructor
public class ProductService {

    private final FranchiseService franchiseService;
    private final FranchiseRepository franchiseRepository;
    private final Clock clock;

    public Product addProduct(String franchiseId, String branchId, CreateProductRequest request) {
        Franchise franchise = franchiseService.getById(franchiseId);
        Branch branch = findBranch(franchise, branchId);
        if (branch.isClosed()) {
            throw new BusinessRuleException(
                    "Branch '%s' is closed and does not accept new products".formatted(branch.getName()));
        }

        String name = text(request.name());
        String sku = upper(request.sku());
        if (branch.hasProductNamed(name)) {
            throw new DuplicateResourceException(
                    "Product '%s' already exists in branch '%s'".formatted(name, branch.getName()));
        }
        if (branch.hasProductWithSku(sku)) {
            throw new DuplicateResourceException(
                    "Product SKU '%s' already exists in branch '%s'".formatted(sku, branch.getName()));
        }

        Instant now = Instant.now(clock);
        String currency = upper(request.currency());
        Product product = Product.builder()
                .id(UUID.randomUUID().toString())
                .sku(sku)
                .name(name)
                .description(text(request.description()))
                .category(text(request.category()))
                .price(request.price())
                .currency(currency != null ? currency : Product.DEFAULT_CURRENCY)
                .stock(request.stock())
                .minimumStock(request.minimumStock() != null ? request.minimumStock() : 0)
                .active(request.active() == null || request.active())
                .createdAt(now)
                .updatedAt(now)
                .build();
        branch.getProducts().add(product);
        franchiseRepository.save(franchise);
        return product;
    }

    public Product getById(String franchiseId, String branchId, String productId) {
        Branch branch = findBranch(franchiseService.getById(franchiseId), branchId);
        return findProduct(branch, productId);
    }

    public void deleteProduct(String franchiseId, String branchId, String productId) {
        Franchise franchise = franchiseService.getById(franchiseId);
        Branch branch = findBranch(franchise, branchId);
        if (!branch.removeProduct(productId)) {
            throw ResourceNotFoundException.product(productId);
        }
        franchiseRepository.save(franchise);
    }

    public Product updateStock(String franchiseId, String branchId, String productId, int stock) {
        Franchise franchise = franchiseService.getById(franchiseId);
        Product product = findProduct(findBranch(franchise, branchId), productId);
        product.setStock(stock);
        product.setUpdatedAt(Instant.now(clock));
        franchiseRepository.save(franchise);
        return product;
    }

    /**
     * Cambia el nombre del producto. Se permite cambiar solo las mayúsculas del propio nombre,
     * pero no usar el de otro producto de la misma sucursal.
     */
    public Product updateName(String franchiseId, String branchId, String productId, String newName) {
        Franchise franchise = franchiseService.getById(franchiseId);
        Branch branch = findBranch(franchise, branchId);
        Product product = findProduct(branch, productId);
        String name = text(newName);
        if (branch.hasOtherProductNamed(productId, name)) {
            throw new DuplicateResourceException(
                    "Product '%s' already exists in branch '%s'".formatted(name, branch.getName()));
        }

        product.setName(name);
        product.setUpdatedAt(Instant.now(clock));
        franchiseRepository.save(franchise);
        return product;
    }

    private Branch findBranch(Franchise franchise, String branchId) {
        return franchise.findBranch(branchId)
                .orElseThrow(() -> ResourceNotFoundException.branch(branchId));
    }

    private Product findProduct(Branch branch, String productId) {
        return branch.findProduct(productId)
                .orElseThrow(() -> ResourceNotFoundException.product(productId));
    }

}
