package com.example.franchisesapi.repository;

import com.example.franchisesapi.model.Franchise;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FranchiseRepository extends MongoRepository<Franchise, String> {

    boolean existsByNameIgnoreCase(String name);

    /** Para renombrar: busca el nombre en las demás franquicias, sin contar la que se renombra. */
    boolean existsByNameIgnoreCaseAndIdNot(String name, String id);

    boolean existsByTaxId(String taxId);

}
