package com.example.franchisesapi.repository;

import com.example.franchisesapi.model.Franchise;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface FranchiseRepository extends MongoRepository<Franchise, String> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByTaxId(String taxId);

}
