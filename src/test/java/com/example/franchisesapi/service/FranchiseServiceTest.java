package com.example.franchisesapi.service;

import com.example.franchisesapi.dto.CreateFranchiseRequest;
import com.example.franchisesapi.exception.DuplicateResourceException;
import com.example.franchisesapi.exception.ResourceNotFoundException;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.model.FranchiseStatus;
import com.example.franchisesapi.repository.FranchiseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
