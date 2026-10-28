package com.example.franchisesapi.controller;

import com.example.franchisesapi.dto.CreateProductRequest;
import com.example.franchisesapi.exception.BusinessRuleException;
import com.example.franchisesapi.exception.DuplicateResourceException;
import com.example.franchisesapi.exception.ResourceNotFoundException;
import com.example.franchisesapi.model.Product;
import com.example.franchisesapi.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String PRODUCTS_URL = "/api/v1/franchises/f1/branches/b1/products";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void addProductReturns201WithLocationAndAllTheFields() throws Exception {
        CreateProductRequest request = new CreateProductRequest("Hamburguesa clásica", "HAM-001",
                "Carne de res", "Hamburguesas", new BigDecimal("18900.00"), "COP", 40, 10, true);
        when(productService.addProduct("f1", "b1", request)).thenReturn(Product.builder()
                .id("p1").sku("HAM-001").name("Hamburguesa clásica").description("Carne de res")
                .category("Hamburguesas").price(new BigDecimal("18900.00")).stock(40).minimumStock(10)
                .build());

        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Hamburguesa clásica",
                                  "sku": "HAM-001",
                                  "description": "Carne de res",
                                  "category": "Hamburguesas",
                                  "price": 18900.00,
                                  "currency": "COP",
                                  "stock": 40,
                                  "minimumStock": 10,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + PRODUCTS_URL + "/p1"))
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.sku").value("HAM-001"))
                .andExpect(jsonPath("$.price").value(18900.00))
                .andExpect(jsonPath("$.currency").value("COP"))
                .andExpect(jsonPath("$.stock").value(40))
                .andExpect(jsonPath("$.minimumStock").value(10))
                .andExpect(jsonPath("$.lowStock").value(false))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void addProductWithInvalidFieldsReturns400WithEveryError() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "sku": "HAM 001",
                                  "price": 0,
                                  "currency": "PESOS",
                                  "stock": -3,
                                  "minimumStock": -1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.sku").exists())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.currency").value("must be a 3-letter ISO 4217 currency code"))
                .andExpect(jsonPath("$.errors.stock").exists())
                .andExpect(jsonPath("$.errors.minimumStock").exists());
        verifyNoInteractions(productService);
    }

    @Test
    void addProductWithoutPriceOrStockReturns400() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Papas"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists())
                .andExpect(jsonPath("$.errors.stock").exists());
    }

    @Test
    void addProductWithMoreThanTwoDecimalsReturns400() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Papas", "price": 7500.999, "stock": 10}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.price").exists());
    }

    @Test
    void addDuplicatedProductReturns409() throws Exception {
        when(productService.addProduct(eq("f1"), eq("b1"), any()))
                .thenThrow(new DuplicateResourceException("Product 'Papas' already exists in branch 'Centro'"));

        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Papas", "price": 7500, "stock": 10}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void addProductToClosedBranchReturns422() throws Exception {
        when(productService.addProduct(eq("f1"), eq("b1"), any()))
                .thenThrow(new BusinessRuleException("Branch 'Centro' is closed and does not accept new products"));

        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Papas", "price": 7500, "stock": 10}
                                """))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.detail").value("Branch 'Centro' is closed and does not accept new products"));
    }

    @Test
    void getByIdReturnsTheProduct() throws Exception {
        when(productService.getById("f1", "b1", "p1")).thenReturn(Product.builder()
                .id("p1").name("Papas").price(new BigDecimal("7500")).stock(10).build());

        mockMvc.perform(get(PRODUCTS_URL + "/p1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Papas"));
    }

    @Test
    void deleteProductReturns204() throws Exception {
        mockMvc.perform(delete(PRODUCTS_URL + "/p1"))
                .andExpect(status().isNoContent());
        verify(productService).deleteProduct("f1", "b1", "p1");
    }

    @Test
    void deleteUnknownProductReturns404() throws Exception {
        doThrow(ResourceNotFoundException.product("missing"))
                .when(productService).deleteProduct("f1", "b1", "missing");

        mockMvc.perform(delete(PRODUCTS_URL + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Product 'missing' not found"));
    }

    @Test
    void updateStockReturnsTheUpdatedProductWithItsLowStockFlag() throws Exception {
        when(productService.updateStock("f1", "b1", "p1", 3)).thenReturn(Product.builder()
                .id("p1").name("Papas").price(new BigDecimal("7500")).stock(3).minimumStock(5).build());

        mockMvc.perform(patch(PRODUCTS_URL + "/p1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stock": 3}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(3))
                .andExpect(jsonPath("$.lowStock").value(true));
    }

    @Test
    void updateStockWithNegativeValueReturns400() throws Exception {
        mockMvc.perform(patch(PRODUCTS_URL + "/p1/stock")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stock": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.stock").exists());
        verifyNoInteractions(productService);
    }

    @Test
    void updateNameReturns200WithTheRenamedProduct() throws Exception {
        when(productService.updateName("f1", "b1", "p1", "Papas a la francesa")).thenReturn(Product.builder()
                .id("p1").name("Papas a la francesa").price(new BigDecimal("7500")).stock(10).build());

        mockMvc.perform(patch(PRODUCTS_URL + "/p1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Papas a la francesa"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("p1"))
                .andExpect(jsonPath("$.name").value("Papas a la francesa"))
                .andExpect(jsonPath("$.stock").value(10));
    }

    @Test
    void updateNameWithTooLongNameReturns400() throws Exception {
        mockMvc.perform(patch(PRODUCTS_URL + "/p1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
        verifyNoInteractions(productService);
    }

    @Test
    void updateNameOfUnknownProductReturns404() throws Exception {
        when(productService.updateName("f1", "b1", "missing", "Papas"))
                .thenThrow(ResourceNotFoundException.product("missing"));

        mockMvc.perform(patch(PRODUCTS_URL + "/missing/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Papas"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateNameToAnExistingNameReturns409() throws Exception {
        when(productService.updateName("f1", "b1", "p1", "Gaseosa"))
                .thenThrow(new DuplicateResourceException("Product 'Gaseosa' already exists in branch 'Centro'"));

        mockMvc.perform(patch(PRODUCTS_URL + "/p1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Gaseosa"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Product 'Gaseosa' already exists in branch 'Centro'"));
    }

}
