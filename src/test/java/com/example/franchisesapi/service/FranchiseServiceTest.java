package com.example.franchisesapi.service;

import com.example.franchisesapi.dto.CreateFranchiseRequest;
import com.example.franchisesapi.dto.TopStockProductResponse;
import com.example.franchisesapi.exception.DuplicateResourceException;
import com.example.franchisesapi.exception.ResourceNotFoundException;
import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.model.BranchStatus;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.model.FranchiseStatus;
import com.example.franchisesapi.model.Product;
import com.example.franchisesapi.repository.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FranchiseServiceTest {

    @Mock
    private FranchiseRepository franchiseRepository;

    @InjectMocks
    private FranchiseService franchiseService;

    @Test
    void createNormalizesTheInputAndStartsActiveWithoutBranches() {
        when(franchiseRepository.existsByNameIgnoreCase("Burger Express")).thenReturn(false);
        when(franchiseRepository.existsByTaxId("900123456-7")).thenReturn(false);
        when(franchiseRepository.save(any(Franchise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Franchise created = franchiseService.create(new CreateFranchiseRequest(
                "  Burger Express ", " Burger Express S.A.S. ", "900123456-7", "   ",
                "Contacto@BurgerExpress.com", "+57 601 555 1234", "https://burgerexpress.com"));

        assertThat(created.getName()).isEqualTo("Burger Express");
        assertThat(created.getLegalName()).isEqualTo("Burger Express S.A.S.");
        assertThat(created.getTaxId()).isEqualTo("900123456-7");
        assertThat(created.getDescription()).as("blank optional text is stored as null").isNull();
        assertThat(created.getEmail()).isEqualTo("contacto@burgerexpress.com");
        assertThat(created.getStatus()).isEqualTo(FranchiseStatus.ACTIVE);
        assertThat(created.getBranches()).isEmpty();
    }

    @Test
    void createWithOnlyTheNameIsAllowed() {
        when(franchiseRepository.save(any(Franchise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Franchise created = franchiseService.create(nameOnly("Burger"));

        assertThat(created.getName()).isEqualTo("Burger");
        assertThat(created.getTaxId()).isNull();
        verify(franchiseRepository, never()).existsByTaxId(any());
    }

    @Test
    void createRejectsADuplicatedName() {
        when(franchiseRepository.existsByNameIgnoreCase("Burger")).thenReturn(true);

        assertThatThrownBy(() -> franchiseService.create(nameOnly("Burger")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Franchise 'Burger' already exists");
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void createRejectsADuplicatedTaxId() {
        when(franchiseRepository.existsByTaxId("900123456-7")).thenReturn(true);

        assertThatThrownBy(() -> franchiseService.create(
                new CreateFranchiseRequest("Pizza", null, "900123456-7", null, null, null, null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("A franchise with tax id '900123456-7' already exists");
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void getTopStockProductsReturnsOneProductPerBranchSkippingEmptyBranches() {
        Branch center = Branch.builder().id("b1").code("BOG-001").name("Centro").products(List.of(
                Product.builder().id("p1").sku("PAP-001").name("Papas").category("Acompañamientos")
                        .stock(10).minimumStock(5).build(),
                Product.builder().id("p2").sku("GAS-001").name("Gaseosa").category("Bebidas")
                        .stock(30).minimumStock(5).build())).build();
        Branch empty = Branch.builder().id("b2").name("Vacía").build();
        Branch north = Branch.builder().id("b3").code("BOG-002").name("Norte").status(BranchStatus.TEMPORARILY_CLOSED)
                .products(List.of(Product.builder().id("p3").sku("HEL-001").name("Helado").category("Postres")
                        .stock(5).minimumStock(8).build())).build();
        when(franchiseRepository.findById("f1")).thenReturn(Optional.of(
                Franchise.builder().id("f1").name("Burger").branches(List.of(center, empty, north)).build()));

        List<TopStockProductResponse> report = franchiseService.getTopStockProducts("f1");

        assertThat(report).containsExactly(
                new TopStockProductResponse("b1", "BOG-001", "Centro", BranchStatus.OPEN,
                        "p2", "GAS-001", "Gaseosa", "Bebidas", 30, 5, false),
                new TopStockProductResponse("b3", "BOG-002", "Norte", BranchStatus.TEMPORARILY_CLOSED,
                        "p3", "HEL-001", "Helado", "Postres", 5, 8, true));
    }

    @Test
    void getTopStockProductsOfAFranchiseWithoutBranchesIsEmpty() {
        when(franchiseRepository.findById("f1"))
                .thenReturn(Optional.of(Franchise.builder().id("f1").name("Burger").build()));

        assertThat(franchiseService.getTopStockProducts("f1")).isEmpty();
    }

    @Test
    void getByIdThrowsWhenTheFranchiseDoesNotExist() {
        when(franchiseRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> franchiseService.getById("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Franchise 'missing' not found");
    }

    private static CreateFranchiseRequest nameOnly(String name) {
        return new CreateFranchiseRequest(name, null, null, null, null, null, null);
    }

}
