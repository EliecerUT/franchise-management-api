package com.example.franchisesapi;

import com.example.franchisesapi.dto.AddressRequest;
import com.example.franchisesapi.dto.BranchResponse;
import com.example.franchisesapi.dto.CreateBranchRequest;
import com.example.franchisesapi.dto.CreateFranchiseRequest;
import com.example.franchisesapi.dto.CreateProductRequest;
import com.example.franchisesapi.dto.FranchiseResponse;
import com.example.franchisesapi.dto.ProductResponse;
import com.example.franchisesapi.dto.TopStockProductResponse;
import com.example.franchisesapi.dto.UpdateNameRequest;
import com.example.franchisesapi.dto.UpdateStockRequest;
import com.example.franchisesapi.model.BranchStatus;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.model.Product;
import com.example.franchisesapi.repository.FranchiseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * Prueba de extremo a extremo: la aplicación completa escuchando en un puerto real,
 * peticiones HTTP reales y un MongoDB real levantado con Testcontainers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class FranchisesApiIntegrationTest {

    private static final String FRANCHISES = "/api/v1/franchises";

    @Autowired
    private RestTestClient client;

    @Autowired
    private FranchiseRepository franchiseRepository;

    @BeforeEach
    void cleanDatabase() {
        franchiseRepository.deleteAll();
    }

    @Test
    void fullFranchiseLifecycle() {
        // Criterio 2: nueva franquicia
        FranchiseResponse franchise = createFranchise(new CreateFranchiseRequest("Burger Express",
                "Burger Express S.A.S.", "900123456-7", "Cadena de hamburguesas artesanales",
                "contacto@burgerexpress.com", "+57 601 555 1234", "https://burgerexpress.com"));
        assertThat(franchise.createdAt()).isNotNull();
        String franchiseUrl = FRANCHISES + "/" + franchise.id();

        // Criterio 3: nuevas sucursales
        BranchResponse center = createBranch(franchiseUrl, new CreateBranchRequest("Centro", "bog-001",
                new AddressRequest("Cra 7 # 12-34", "Bogotá", "Cundinamarca", "Colombia", "110311"),
                "+57 601 555 0001", "centro@burgerexpress.com", "Laura Gómez", LocalDate.of(2020, 3, 15), null));
        BranchResponse north = createBranch(franchiseUrl, branchNamed("Norte", "BOG-002"));
        createBranch(franchiseUrl, branchNamed("Sur", "BOG-003"));
        assertThat(center.code()).isEqualTo("BOG-001");
        String centerProducts = franchiseUrl + "/branches/" + center.id() + "/products";
        String northProducts = franchiseUrl + "/branches/" + north.id() + "/products";

        // Criterio 4: nuevos productos
        ProductResponse burger = createProduct(centerProducts, new CreateProductRequest("Hamburguesa clásica",
                "HAM-001", "Carne de res y queso cheddar", "Hamburguesas", new BigDecimal("18900.00"),
                null, 10, 5, null));
        createProduct(centerProducts, product("Gaseosa", "GAS-001", "4500.00", 30));
        createProduct(northProducts, product("Helado", "HEL-001", "6000.00", 5));
        ProductResponse cone = createProduct(northProducts, product("Cono", "CON-001", "3000.00", 8));
        assertThat(burger.currency()).isEqualTo("COP");
        assertThat(burger.lowStock()).isFalse();

        // Criterio 6: modificar stock (queda por debajo del mínimo y luego se repone)
        client.patch().uri(centerProducts + "/" + burger.id() + "/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateStockRequest(3))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.lowStock").isEqualTo(true);
        client.patch().uri(centerProducts + "/" + burger.id() + "/stock")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateStockRequest(50))
                .exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.stock").isEqualTo(50);

        // Criterio 5: eliminar producto
        client.delete().uri(northProducts + "/" + cone.id())
                .exchange()
                .expectStatus().isNoContent();
        client.get().uri(northProducts + "/" + cone.id())
                .exchange()
                .expectStatus().isNotFound();

        // Criterio 7: producto con más stock por sucursal ("Sur" no tiene productos)
        List<TopStockProductResponse> report = client.get().uri(franchiseUrl + "/top-stock-products")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<List<TopStockProductResponse>>() {
                })
                .returnResult()
                .getResponseBody();

        assertThat(report)
                .extracting(TopStockProductResponse::branchCode, TopStockProductResponse::branchName,
                        TopStockProductResponse::sku, TopStockProductResponse::stock)
                .containsExactly(
                        tuple("BOG-001", "Centro", "HAM-001", 50),
                        tuple("BOG-002", "Norte", "HEL-001", 5));

        // Criterio 8: todo quedó persistido en MongoDB
        Franchise stored = franchiseRepository.findById(franchise.id()).orElseThrow();
        assertThat(stored.getTaxId()).isEqualTo("900123456-7");
        assertThat(stored.getCreatedAt()).as("dates returned by the API match the stored ones")
                .isEqualTo(franchise.createdAt());
        assertThat(stored.findBranch(center.id()).orElseThrow().getCreatedAt()).isEqualTo(center.createdAt());
        assertThat(stored.getBranches()).hasSize(3);
        assertThat(stored.getUpdatedAt()).isAfterOrEqualTo(stored.getCreatedAt());
        Product storedBurger = stored.findBranch(center.id()).orElseThrow().findProduct(burger.id()).orElseThrow();
        assertThat(storedBurger.getPrice()).isEqualByComparingTo("18900.00");
        assertThat(storedBurger.getUpdatedAt()).isAfter(storedBurger.getCreatedAt());
        assertThat(stored.findBranch(north.id()).orElseThrow().getProducts())
                .extracting(Product::getName).containsExactly("Helado");
    }

    @Test
    void duplicatedFranchiseNameOrTaxIdReturns409() {
        createFranchise(new CreateFranchiseRequest("Burger", null, "900123456-7", null, null, null, null));

        client.post().uri(FRANCHISES)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateFranchiseRequest("burger", null, null, null, null, null, null))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON);
        client.post().uri(FRANCHISES)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateFranchiseRequest("Pizza", null, "900123456-7", null, null, null, null))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody().jsonPath("$.detail").isEqualTo("A franchise with tax id '900123456-7' already exists");
    }

    @Test
    void renamingAFranchiseKeepsTheRestOfItsData() {
        FranchiseResponse burger = createFranchise(
                new CreateFranchiseRequest("Burger", null, "900123456-7", null, null, null, null));
        createFranchise(franchiseNamed("Pizza"));
        String renameUrl = FRANCHISES + "/" + burger.id() + "/name";

        client.patch().uri(renameUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateNameRequest("Burger House"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Burger House")
                .jsonPath("$.taxId").isEqualTo("900123456-7");
        client.patch().uri(renameUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateNameRequest("PIZZA"))
                .exchange()
                .expectStatus().isEqualTo(409);

        Franchise stored = franchiseRepository.findById(burger.id()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Burger House");
        assertThat(stored.getUpdatedAt()).isAfter(stored.getCreatedAt());
    }

    @Test
    void renamingABranchKeepsItsProducts() {
        FranchiseResponse franchise = createFranchise(franchiseNamed("Burger"));
        String franchiseUrl = FRANCHISES + "/" + franchise.id();
        BranchResponse center = createBranch(franchiseUrl, branchNamed("Centro", "BOG-001"));
        createBranch(franchiseUrl, branchNamed("Norte", "BOG-002"));
        String centerUrl = franchiseUrl + "/branches/" + center.id();
        createProduct(centerUrl + "/products", product("Papas", "PAP-001", "7500.00", 10));

        client.patch().uri(centerUrl + "/name")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateNameRequest("Centro Histórico"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Centro Histórico")
                .jsonPath("$.code").isEqualTo("BOG-001")
                .jsonPath("$.products[0].name").isEqualTo("Papas");
        client.patch().uri(centerUrl + "/name")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateNameRequest("norte"))
                .exchange()
                .expectStatus().isEqualTo(409);

        assertThat(franchiseRepository.findById(franchise.id()).orElseThrow()
                .findBranch(center.id()).orElseThrow().getName()).isEqualTo("Centro Histórico");
    }

    @Test
    void renamingAProductKeepsItsStockAndPrice() {
        FranchiseResponse franchise = createFranchise(franchiseNamed("Burger"));
        BranchResponse center = createBranch(FRANCHISES + "/" + franchise.id(), branchNamed("Centro", null));
        String productsUrl = FRANCHISES + "/" + franchise.id() + "/branches/" + center.id() + "/products";
        ProductResponse fries = createProduct(productsUrl, product("Papas", "PAP-001", "7500.00", 10));
        createProduct(productsUrl, product("Gaseosa", "GAS-001", "4500.00", 30));

        client.patch().uri(productsUrl + "/" + fries.id() + "/name")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateNameRequest("Papas a la francesa"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.name").isEqualTo("Papas a la francesa")
                .jsonPath("$.sku").isEqualTo("PAP-001")
                .jsonPath("$.stock").isEqualTo(10);
        client.patch().uri(productsUrl + "/" + fries.id() + "/name")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UpdateNameRequest("GASEOSA"))
                .exchange()
                .expectStatus().isEqualTo(409);

        Product stored = franchiseRepository.findById(franchise.id()).orElseThrow()
                .findBranch(center.id()).orElseThrow().findProduct(fries.id()).orElseThrow();
        assertThat(stored.getName()).isEqualTo("Papas a la francesa");
        assertThat(stored.getPrice()).isEqualByComparingTo("7500.00");
    }

    @Test
    void closedBranchDoesNotAcceptProducts() {
        FranchiseResponse franchise = createFranchise(franchiseNamed("Burger"));
        BranchResponse closed = createBranch(FRANCHISES + "/" + franchise.id(),
                new CreateBranchRequest("Antigua", null, null, null, null, null, null, BranchStatus.CLOSED));

        client.post().uri(FRANCHISES + "/" + franchise.id() + "/branches/" + closed.id() + "/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(product("Papas", null, "7500.00", 10))
                .exchange()
                .expectStatus().isEqualTo(422)
                .expectBody().jsonPath("$.detail").isEqualTo("Branch 'Antigua' is closed and does not accept new products");
    }

    @Test
    void unknownFranchiseReturns404() {
        client.post().uri(FRANCHISES + "/does-not-exist/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .body(branchNamed("Centro", null))
                .exchange()
                .expectStatus().isNotFound()
                .expectBody().jsonPath("$.detail").isEqualTo("Franchise 'does-not-exist' not found");
    }

    @Test
    void invalidProductReturns400AndIsNotStored() {
        FranchiseResponse franchise = createFranchise(franchiseNamed("Burger"));
        BranchResponse center = createBranch(FRANCHISES + "/" + franchise.id(), branchNamed("Centro", null));

        client.post().uri(FRANCHISES + "/" + franchise.id() + "/branches/" + center.id() + "/products")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CreateProductRequest("Papas", null, null, null, new BigDecimal("-1"), "PESOS", -1, null, null))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.errors.price").exists()
                .jsonPath("$.errors.currency").exists()
                .jsonPath("$.errors.stock").exists();

        assertThat(franchiseRepository.findById(franchise.id()).orElseThrow()
                .findBranch(center.id()).orElseThrow().getProducts()).isEmpty();
    }

    private FranchiseResponse createFranchise(CreateFranchiseRequest request) {
        return client.post().uri(FRANCHISES)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().exists("Location")
                .expectBody(FranchiseResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private BranchResponse createBranch(String franchiseUrl, CreateBranchRequest request) {
        return client.post().uri(franchiseUrl + "/branches")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(BranchResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private ProductResponse createProduct(String productsUrl, CreateProductRequest request) {
        return client.post().uri(productsUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(ProductResponse.class)
                .returnResult()
                .getResponseBody();
    }

    private static CreateFranchiseRequest franchiseNamed(String name) {
        return new CreateFranchiseRequest(name, null, null, null, null, null, null);
    }

    private static CreateBranchRequest branchNamed(String name, String code) {
        return new CreateBranchRequest(name, code, null, null, null, null, null, null);
    }

    private static CreateProductRequest product(String name, String sku, String price, int stock) {
        return new CreateProductRequest(name, sku, null, null, new BigDecimal(price), null, stock, null, null);
    }

}
