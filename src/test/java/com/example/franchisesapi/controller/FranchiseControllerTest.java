package com.example.franchisesapi.controller;

import com.example.franchisesapi.dto.CreateFranchiseRequest;
import com.example.franchisesapi.dto.TopStockProductResponse;
import com.example.franchisesapi.exception.DuplicateResourceException;
import com.example.franchisesapi.exception.ResourceNotFoundException;
import com.example.franchisesapi.model.Address;
import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.model.BranchStatus;
import com.example.franchisesapi.model.Franchise;
import com.example.franchisesapi.model.Product;
import com.example.franchisesapi.service.FranchiseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FranchiseController.class)
class FranchiseControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-27T15:04:05Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FranchiseService franchiseService;

    @Test
    void createReturns201WithLocationAndAllTheFields() throws Exception {
        CreateFranchiseRequest request = new CreateFranchiseRequest("Burger Express", "Burger Express S.A.S.",
                "900123456-7", "Hamburguesas artesanales", "contacto@burgerexpress.com", "+57 601 555 1234",
                "https://burgerexpress.com");
        when(franchiseService.create(request)).thenReturn(Franchise.builder()
                .id("f1").name("Burger Express").legalName("Burger Express S.A.S.").taxId("900123456-7")
                .description("Hamburguesas artesanales").email("contacto@burgerexpress.com")
                .phone("+57 601 555 1234").website("https://burgerexpress.com")
                .createdAt(NOW).updatedAt(NOW)
                .build());

        mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Burger Express",
                                  "legalName": "Burger Express S.A.S.",
                                  "taxId": "900123456-7",
                                  "description": "Hamburguesas artesanales",
                                  "email": "contacto@burgerexpress.com",
                                  "phone": "+57 601 555 1234",
                                  "website": "https://burgerexpress.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/franchises/f1"))
                .andExpect(jsonPath("$.id").value("f1"))
                .andExpect(jsonPath("$.legalName").value("Burger Express S.A.S."))
                .andExpect(jsonPath("$.taxId").value("900123456-7"))
                .andExpect(jsonPath("$.website").value("https://burgerexpress.com"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.branches").isEmpty())
                .andExpect(jsonPath("$.createdAt").value("2026-10-27T15:04:05Z"));
    }

    @Test
    void createWithOnlyTheNameReturns201() throws Exception {
        when(franchiseService.create(any())).thenReturn(Franchise.builder().id("f1").name("Burger").build());

        mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Burger"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.taxId").doesNotExist());
    }

    @Test
    void createWithInvalidFieldsReturns400WithEveryError() throws Exception {
        mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "taxId": "#1",
                                  "email": "not-an-email",
                                  "phone": "abc",
                                  "website": "burgerexpress.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.taxId").value("must contain 5 to 30 letters, digits, dots or hyphens"))
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.phone").exists())
                .andExpect(jsonPath("$.errors.website").value("must be a valid http or https URL"));
        verifyNoInteractions(franchiseService);
    }

    @Test
    void createWithDuplicatedNameReturns409() throws Exception {
        when(franchiseService.create(any())).thenThrow(new DuplicateResourceException("Franchise 'Burger' already exists"));

        mockMvc.perform(post("/api/v1/franchises")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Burger"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Franchise 'Burger' already exists"));
    }

    @Test
    void getByIdReturnsTheWholeFranchise() throws Exception {
        Product burger = Product.builder().id("p1").sku("HAM-001").name("Hamburguesa").category("Hamburguesas")
                .price(new BigDecimal("18900.00")).stock(8).minimumStock(10).build();
        Branch center = Branch.builder().id("b1").code("BOG-001").name("Centro")
                .address(Address.builder().street("Cra 7 # 12-34").city("Bogotá").country("Colombia").build())
                .openingDate(LocalDate.of(2020, 3, 15))
                .products(List.of(burger)).build();
        when(franchiseService.getById("f1"))
                .thenReturn(Franchise.builder().id("f1").name("Burger").branches(List.of(center)).build());

        mockMvc.perform(get("/api/v1/franchises/f1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.branches[0].code").value("BOG-001"))
                .andExpect(jsonPath("$.branches[0].address.city").value("Bogotá"))
                .andExpect(jsonPath("$.branches[0].openingDate").value("2020-03-15"))
                .andExpect(jsonPath("$.branches[0].status").value("OPEN"))
                .andExpect(jsonPath("$.branches[0].products[0].price").value(18900.00))
                .andExpect(jsonPath("$.branches[0].products[0].currency").value("COP"))
                .andExpect(jsonPath("$.branches[0].products[0].lowStock").value(true));
    }

    @Test
    void getByIdOfUnknownFranchiseReturns404() throws Exception {
        when(franchiseService.getById("missing")).thenThrow(ResourceNotFoundException.franchise("missing"));

        mockMvc.perform(get("/api/v1/franchises/missing"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getTopStockProductsReturnsTheProductAndItsBranch() throws Exception {
        when(franchiseService.getTopStockProducts("f1")).thenReturn(List.of(
                new TopStockProductResponse("b1", "BOG-001", "Centro", BranchStatus.OPEN,
                        "p2", "GAS-001", "Gaseosa", "Bebidas", 30, 5, false),
                new TopStockProductResponse("b3", "BOG-002", "Norte", BranchStatus.TEMPORARILY_CLOSED,
                        "p3", "HEL-001", "Helado", "Postres", 5, 8, true)));

        mockMvc.perform(get("/api/v1/franchises/f1/top-stock-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].branchCode").value("BOG-001"))
                .andExpect(jsonPath("$[0].branchName").value("Centro"))
                .andExpect(jsonPath("$[0].productName").value("Gaseosa"))
                .andExpect(jsonPath("$[0].sku").value("GAS-001"))
                .andExpect(jsonPath("$[0].stock").value(30))
                .andExpect(jsonPath("$[1].branchStatus").value("TEMPORARILY_CLOSED"))
                .andExpect(jsonPath("$[1].lowStock").value(true));
    }

    @Test
    void getTopStockProductsOfUnknownFranchiseReturns404() throws Exception {
        when(franchiseService.getTopStockProducts("missing")).thenThrow(ResourceNotFoundException.franchise("missing"));

        mockMvc.perform(get("/api/v1/franchises/missing/top-stock-products"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findAllListsEveryFranchise() throws Exception {
        when(franchiseService.findAll()).thenReturn(List.of(
                Franchise.builder().id("f1").name("Burger").build(),
                Franchise.builder().id("f2").name("Pizza").build()));

        mockMvc.perform(get("/api/v1/franchises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].name").value("Pizza"));
    }

}
