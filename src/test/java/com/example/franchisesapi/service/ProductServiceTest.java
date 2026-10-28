package com.example.franchisesapi.service;

import com.example.franchisesapi.dto.CreateProductRequest;
import com.example.franchisesapi.exception.BusinessRuleException;
import com.example.franchisesapi.exception.DuplicateResourceException;
import com.example.franchisesapi.exception.ResourceNotFoundException;
import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.model.BranchStatus;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.model.Product;
import com.example.franchisesapi.repository.FranchiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Instant CREATED = Instant.parse("2026-01-10T08:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-27T15:04:05Z");

    @Mock
    private FranchiseService franchiseService;

    @Mock
    private FranchiseRepository franchiseRepository;

    private ProductService productService;

    private Franchise franchise;
    private Branch branch;

    @BeforeEach
    void setUp() {
        productService = new ProductService(franchiseService, franchiseRepository, Clock.fixed(NOW, ZoneOffset.UTC));
        Product fries = Product.builder().id("p1").sku("PAP-001").name("Papas").price(new BigDecimal("7500.00"))
                .stock(10).createdAt(CREATED).updatedAt(CREATED).build();
        branch = Branch.builder().id("b1").name("Centro").products(new ArrayList<>(List.of(fries))).build();
        franchise = Franchise.builder().id("f1").name("Burger").branches(new ArrayList<>(List.of(branch))).build();
        when(franchiseService.getById("f1")).thenReturn(franchise);
    }

    @Test
    void addProductNormalizesTheInputAndSavesTheFranchise() {
        Product product = productService.addProduct("f1", "b1", new CreateProductRequest(
                " Hamburguesa clásica ", "ham-001", "Carne de res", " Hamburguesas ",
                new BigDecimal("18900.00"), "usd", 40, 10, null));

        assertThat(product.getId()).isNotBlank();
        assertThat(product.getName()).isEqualTo("Hamburguesa clásica");
        assertThat(product.getSku()).isEqualTo("HAM-001");
        assertThat(product.getCategory()).isEqualTo("Hamburguesas");
        assertThat(product.getPrice()).isEqualByComparingTo("18900.00");
        assertThat(product.getCurrency()).isEqualTo("USD");
        assertThat(product.getStock()).isEqualTo(40);
        assertThat(product.getMinimumStock()).isEqualTo(10);
        assertThat(product.isActive()).as("active by default").isTrue();
        assertThat(product.getCreatedAt()).isEqualTo(NOW);
        assertThat(branch.getProducts()).hasSize(2).contains(product);
        verify(franchiseRepository).save(franchise);
    }

    @Test
    void addProductAppliesTheDefaults() {
        Product product = productService.addProduct("f1", "b1",
                required("Gaseosa", new BigDecimal("4500"), 25));

        assertThat(product.getSku()).isNull();
        assertThat(product.getCurrency()).isEqualTo("COP");
        assertThat(product.getMinimumStock()).isZero();
        assertThat(product.isActive()).isTrue();
    }

    @Test
    void addProductRejectsADuplicatedNameOrSkuInTheSameBranch() {
        assertThatThrownBy(() -> productService.addProduct("f1", "b1", required("PAPAS", BigDecimal.TEN, 5)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Product 'PAPAS' already exists in branch 'Centro'");
        assertThatThrownBy(() -> productService.addProduct("f1", "b1", new CreateProductRequest(
                "Papas grandes", "pap-001", null, null, BigDecimal.TEN, null, 5, null, null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Product SKU 'PAP-001' already exists in branch 'Centro'");
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void addProductToAClosedBranchIsRejected() {
        branch.setStatus(BranchStatus.CLOSED);

        assertThatThrownBy(() -> productService.addProduct("f1", "b1", required("Gaseosa", BigDecimal.TEN, 5)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Branch 'Centro' is closed and does not accept new products");
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void addProductToUnknownBranchFails() {
        assertThatThrownBy(() -> productService.addProduct("f1", "missing", required("Gaseosa", BigDecimal.TEN, 5)))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Branch 'missing' not found");
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void deleteProductRemovesItAndSavesTheFranchise() {
        productService.deleteProduct("f1", "b1", "p1");

        assertThat(branch.getProducts()).isEmpty();
        verify(franchiseRepository).save(franchise);
    }

    @Test
    void deleteUnknownProductFails() {
        assertThatThrownBy(() -> productService.deleteProduct("f1", "b1", "missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product 'missing' not found");
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void updateStockChangesTheStockAndTheModificationDate() {
        Product updated = productService.updateStock("f1", "b1", "p1", 42);

        assertThat(updated.getStock()).isEqualTo(42);
        assertThat(updated.getCreatedAt()).isEqualTo(CREATED);
        assertThat(updated.getUpdatedAt()).isEqualTo(NOW);
        assertThat(branch.getProducts().getFirst().getStock()).isEqualTo(42);
        verify(franchiseRepository).save(franchise);
    }

    @Test
    void updateStockOfUnknownProductFails() {
        assertThatThrownBy(() -> productService.updateStock("f1", "b1", "missing", 42))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(franchiseRepository, never()).save(any());
    }

    private static CreateProductRequest required(String name, BigDecimal price, int stock) {
        return new CreateProductRequest(name, null, null, null, price, null, stock, null, null);
    }

}
