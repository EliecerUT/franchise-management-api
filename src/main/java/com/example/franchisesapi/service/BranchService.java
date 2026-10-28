package com.example.franchisesapi.service;

import com.example.franchisesapi.dto.AddressRequest;
import com.example.franchisesapi.dto.CreateBranchRequest;
import com.example.franchisesapi.exception.BusinessRuleException;
import com.example.franchisesapi.exception.DuplicateResourceException;
import com.example.franchisesapi.exception.ResourceNotFoundException;
import com.example.franchisesapi.model.Address;
import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.model.BranchStatus;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.repository.FranchiseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import static com.example.franchisesapi.service.InputNormalizer.lower;
import static com.example.franchisesapi.service.InputNormalizer.text;
import static com.example.franchisesapi.service.InputNormalizer.upper;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final FranchiseService franchiseService;
    private final FranchiseRepository franchiseRepository;
    private final Clock clock;

    /**
     * La sucursal vive dentro del documento de la franquicia: se carga la franquicia,
     * se añade la sucursal a su lista y se guarda el documento completo.
     */
    public Branch addBranch(String franchiseId, CreateBranchRequest request) {
        Franchise franchise = franchiseService.getById(franchiseId);
        if (!franchise.isActive()) {
            throw new BusinessRuleException(
                    "Franchise '%s' is inactive and does not accept new branches".formatted(franchise.getName()));
        }

        String name = text(request.name());
        String code = upper(request.code());
        if (franchise.hasBranchNamed(name)) {
            throw new DuplicateResourceException(
                    "Branch '%s' already exists in franchise '%s'".formatted(name, franchise.getName()));
        }
        if (franchise.hasBranchWithCode(code)) {
            throw new DuplicateResourceException(
                    "Branch code '%s' already exists in franchise '%s'".formatted(code, franchise.getName()));
        }

        Instant now = Instant.now(clock);
        Branch branch = Branch.builder()
                .id(UUID.randomUUID().toString())
                .code(code)
                .name(name)
                .address(toAddress(request.address()))
                .phone(text(request.phone()))
                .email(lower(request.email()))
                .managerName(text(request.managerName()))
                .openingDate(request.openingDate())
                .status(request.status() != null ? request.status() : BranchStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build();
        franchise.getBranches().add(branch);
        franchiseRepository.save(franchise);
        return branch;
    }

    public Branch getById(String franchiseId, String branchId) {
        return franchiseService.getById(franchiseId)
                .findBranch(branchId)
                .orElseThrow(() -> ResourceNotFoundException.branch(branchId));
    }

    private static Address toAddress(AddressRequest request) {
        if (request == null) {
            return null;
        }
        return Address.builder()
                .street(text(request.street()))
                .city(text(request.city()))
                .state(text(request.state()))
                .country(text(request.country()))
                .postalCode(text(request.postalCode()))
                .build();
    }

}
