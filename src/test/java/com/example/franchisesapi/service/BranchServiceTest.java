package com.example.franchisesapi.service;

import com.example.franchisesapi.dto.AddressRequest;
import com.example.franchisesapi.dto.CreateBranchRequest;
import com.example.franchisesapi.exception.BusinessRuleException;
import com.example.franchisesapi.exception.DuplicateResourceException;
import com.example.franchisesapi.exception.ResourceNotFoundException;
import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.model.BranchStatus;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.model.FranchiseStatus;
import com.example.franchisesapi.repository.FranchiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
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
class BranchServiceTest {

    private static final Instant NOW = Instant.parse("2026-10-27T15:04:05Z");

    @Mock
    private FranchiseService franchiseService;

    @Mock
    private FranchiseRepository franchiseRepository;

    private BranchService branchService;

    @BeforeEach
    void setUp() {
        branchService = new BranchService(franchiseService, franchiseRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void addBranchNormalizesTheInputAndSavesTheFranchise() {
        Franchise franchise = Franchise.builder().id("f1").name("Burger").build();
        when(franchiseService.getById("f1")).thenReturn(franchise);

        Branch branch = branchService.addBranch("f1", new CreateBranchRequest(
                " Centro ", "bog-001",
                new AddressRequest(" Cra 7 # 12-34 ", "Bogotá", "Cundinamarca", "Colombia", " "),
                "+57 601 555 0001", "Centro@BurgerExpress.com", "Laura Gómez",
                LocalDate.of(2020, 3, 15), null));

        assertThat(branch.getId()).isNotBlank();
        assertThat(branch.getName()).isEqualTo("Centro");
        assertThat(branch.getCode()).isEqualTo("BOG-001");
        assertThat(branch.getAddress().getStreet()).isEqualTo("Cra 7 # 12-34");
        assertThat(branch.getAddress().getPostalCode()).isNull();
        assertThat(branch.getEmail()).isEqualTo("centro@burgerexpress.com");
        assertThat(branch.getStatus()).as("OPEN by default").isEqualTo(BranchStatus.OPEN);
        assertThat(branch.getCreatedAt()).isEqualTo(NOW);
        assertThat(branch.getUpdatedAt()).isEqualTo(NOW);
        assertThat(branch.getProducts()).isEmpty();
        assertThat(franchise.getBranches()).containsExactly(branch);
        verify(franchiseRepository).save(franchise);
    }

    @Test
    void addBranchWithOnlyTheNameAndAnExplicitStatus() {
        Franchise franchise = Franchise.builder().id("f1").name("Burger").build();
        when(franchiseService.getById("f1")).thenReturn(franchise);

        Branch branch = branchService.addBranch("f1", new CreateBranchRequest(
                "Norte", null, null, null, null, null, null, BranchStatus.TEMPORARILY_CLOSED));

        assertThat(branch.getCode()).isNull();
        assertThat(branch.getAddress()).isNull();
        assertThat(branch.getStatus()).isEqualTo(BranchStatus.TEMPORARILY_CLOSED);
    }

    @Test
    void addBranchRejectsADuplicatedNameOrCodeInTheSameFranchise() {
        Franchise franchise = Franchise.builder().id("f1").name("Burger")
                .branches(new ArrayList<>(List.of(Branch.builder().id("b1").code("BOG-001").name("Centro").build())))
                .build();
        when(franchiseService.getById("f1")).thenReturn(franchise);

        assertThatThrownBy(() -> branchService.addBranch("f1", nameAndCode("centro", null)))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Branch 'centro' already exists in franchise 'Burger'");
        assertThatThrownBy(() -> branchService.addBranch("f1", nameAndCode("Norte", "bog-001")))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("Branch code 'BOG-001' already exists in franchise 'Burger'");
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void addBranchToAnInactiveFranchiseIsRejected() {
        when(franchiseService.getById("f1")).thenReturn(
                Franchise.builder().id("f1").name("Burger").status(FranchiseStatus.INACTIVE).build());

        assertThatThrownBy(() -> branchService.addBranch("f1", nameAndCode("Centro", null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Franchise 'Burger' is inactive and does not accept new branches");
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void addBranchToUnknownFranchiseFails() {
        when(franchiseService.getById("missing")).thenThrow(ResourceNotFoundException.franchise("missing"));

        assertThatThrownBy(() -> branchService.addBranch("missing", nameAndCode("Centro", null)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(franchiseRepository, never()).save(any());
    }

    @Test
    void getByIdThrowsWhenTheBranchDoesNotExist() {
        when(franchiseService.getById("f1")).thenReturn(Franchise.builder().id("f1").name("Burger").build());

        assertThatThrownBy(() -> branchService.getById("f1", "missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Branch 'missing' not found");
    }

    private static CreateBranchRequest nameAndCode(String name, String code) {
        return new CreateBranchRequest(name, code, null, null, null, null, null, null);
    }

}
