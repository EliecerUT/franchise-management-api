package com.example.franchisesapi.repository;

import com.example.franchisesapi.TestcontainersConfiguration;
import com.example.franchisesapi.config.MongoConfig;
import com.example.franchisesapi.model.Address;
import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.model.BranchStatus;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.model.Product;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.mongodb.test.autoconfigure.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataMongoTest
@Import({TestcontainersConfiguration.class, MongoConfig.class})
@Testcontainers(disabledWithoutDocker = true)
class FranchiseRepositoryTest {

    @Autowired
    private FranchiseRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void cleanUp() {
        repository.deleteAll();
    }

    @Test
    void savesTheWholeAggregateInOneDocument() {
        Franchise saved = repository.save(sampleFranchise("Burger", "900123456-7"));

        Franchise found = repository.findById(saved.getId()).orElseThrow();

        assertThat(found.getLegalName()).isEqualTo("Burger S.A.S.");
        Branch branch = found.getBranches().getFirst();
        assertThat(branch.getAddress().getCity()).isEqualTo("Bogotá");
        assertThat(branch.getOpeningDate()).isEqualTo(LocalDate.of(2020, 3, 15));
        assertThat(branch.getStatus()).isEqualTo(BranchStatus.OPEN);
        Product product = branch.getProducts().getFirst();
        assertThat(product.getPrice()).isEqualByComparingTo("18900.50");
        assertThat(product.getCurrency()).isEqualTo("COP");
        assertThat(product.getMinimumStock()).isEqualTo(5);
    }

    @Test
    void pricesAreStoredAsDecimal128() {
        repository.save(sampleFranchise("Burger", null));

        Document raw = mongoTemplate.getCollection("franchises").find().first();
        Document product = raw.getList("branches", Document.class).getFirst()
                .getList("products", Document.class).getFirst();

        assertThat(product.get("price")).isEqualTo(Decimal128.parse("18900.50"));
    }

    @Test
    void auditingFillsCreatedAtAndUpdatedAt() {
        Franchise saved = repository.save(Franchise.builder().name("Burger").build());
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        saved.setDescription("Hamburguesas artesanales");
        Franchise updated = repository.save(saved);

        assertThat(updated.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isAfterOrEqualTo(updated.getCreatedAt());
    }

    @Test
    void existsQueriesFindByNameIgnoringCaseAndByTaxId() {
        repository.save(Franchise.builder().name("Burger").taxId("900123456-7").build());

        assertThat(repository.existsByNameIgnoreCase("burger")).isTrue();
        assertThat(repository.existsByNameIgnoreCase("Pizza")).isFalse();
        assertThat(repository.existsByTaxId("900123456-7")).isTrue();
        assertThat(repository.existsByTaxId("800000000-1")).isFalse();
    }

    @Test
    void franchiseNameIsUniqueIgnoringCase() {
        repository.save(Franchise.builder().name("Burger").build());

        assertThatThrownBy(() -> repository.save(Franchise.builder().name("BURGER").build()))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void taxIdIsUniqueButOptional() {
        repository.save(Franchise.builder().name("Burger").taxId("900123456-7").build());
        repository.save(Franchise.builder().name("Pizza").build());
        repository.save(Franchise.builder().name("Tacos").build());

        assertThatThrownBy(() -> repository.save(Franchise.builder().name("Sushi").taxId("900123456-7").build()))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    void savingAStaleCopyFails() {
        Franchise saved = repository.save(Franchise.builder().name("Burger").build());
        Franchise firstCopy = repository.findById(saved.getId()).orElseThrow();
        Franchise secondCopy = repository.findById(saved.getId()).orElseThrow();

        firstCopy.setName("Burger House");
        repository.save(firstCopy);
        secondCopy.setName("Burger King");

        assertThatThrownBy(() -> repository.save(secondCopy))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    private static Franchise sampleFranchise(String name, String taxId) {
        Product burger = Product.builder()
                .id("p1").sku("HAM-001").name("Hamburguesa").category("Comidas")
                .price(new BigDecimal("18900.50")).stock(40).minimumStock(5)
                .build();
        Branch center = Branch.builder()
                .id("b1").code("BOG-001").name("Centro")
                .address(Address.builder().street("Cra 7 # 12-34").city("Bogotá").country("Colombia").build())
                .openingDate(LocalDate.of(2020, 3, 15))
                .products(List.of(burger))
                .build();
        return Franchise.builder()
                .name(name).legalName("Burger S.A.S.").taxId(taxId)
                .branches(List.of(center))
                .build();
    }

}
