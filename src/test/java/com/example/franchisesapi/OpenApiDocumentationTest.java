package com.example.franchisesapi;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica la especificación OpenAPI que genera springdoc en /v3/api-docs.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class OpenApiDocumentationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void describesTheApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Franchises API"))
                .andExpect(jsonPath("$.info.version").value("v1"))
                .andExpect(jsonPath("$.tags[*].name").value(hasItems("Franquicias", "Sucursales", "Productos")));
    }

    @Test
    void declaresTheReusableErrorResponses() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.status.type").value("integer"))
                .andExpect(jsonPath("$.components.schemas.ProblemDetail.properties.errors").exists())
                .andExpect(jsonPath("$.components.responses.BadRequest.content['application/problem+json'].schema.$ref")
                        .value("#/components/schemas/ProblemDetail"))
                .andExpect(jsonPath("$.components.responses.NotFound").exists())
                .andExpect(jsonPath("$.components.responses.Conflict").exists())
                .andExpect(jsonPath("$.components.responses.UnprocessableContent").exists());
    }

    @Test
    void documentsTheFranchiseEndpoints() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath("$.paths['/api/v1/franchises'].post.tags[0]").value("Franquicias"))
                .andExpect(jsonPath("$.paths['/api/v1/franchises'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath("$.paths['/api/v1/franchises'].post.responses['201'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/FranchiseResponse"))
                .andExpect(jsonPath("$.paths['/api/v1/franchises'].post.responses['400'].$ref")
                        .value("#/components/responses/BadRequest"))
                .andExpect(jsonPath("$.paths['/api/v1/franchises'].post.responses['409'].$ref")
                        .value("#/components/responses/Conflict"))
                .andExpect(jsonPath("$.paths['/api/v1/franchises/{franchiseId}'].get.responses['404'].$ref")
                        .value("#/components/responses/NotFound"))
                .andExpect(jsonPath("$.components.schemas.CreateFranchiseRequest.required").value(hasItems("name")))
                .andExpect(jsonPath("$.components.schemas.CreateFranchiseRequest.properties.name.example")
                        .value("Burger Express"));
    }

    @Test
    void documentsTheFranchiseRename() throws Exception {
        String rename = "$.paths['/api/v1/franchises/{franchiseId}/name'].patch";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(rename + ".tags[0]").value("Franquicias"))
                .andExpect(jsonPath(rename + ".requestBody.content['application/json'].schema.$ref")
                        .value("#/components/schemas/UpdateNameRequest"))
                .andExpect(jsonPath(rename + ".responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/FranchiseResponse"))
                .andExpect(jsonPath(rename + ".responses['409'].$ref").value("#/components/responses/Conflict"))
                .andExpect(jsonPath("$.components.schemas.UpdateNameRequest.required").value(hasItems("name")));
    }

    @Test
    void documentsTheBranchEndpoints() throws Exception {
        String addBranch = "$.paths['/api/v1/franchises/{franchiseId}/branches'].post";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(addBranch + ".tags[0]").value("Sucursales"))
                .andExpect(jsonPath(addBranch + ".parameters[0].description").value("Id de la franquicia"))
                .andExpect(jsonPath(addBranch + ".responses['201'].headers.Location").exists())
                .andExpect(jsonPath(addBranch + ".responses['422'].$ref").value("#/components/responses/UnprocessableContent"))
                .andExpect(jsonPath("$.components.schemas.AddressRequest.required").value(hasItems("city", "country")))
                .andExpect(jsonPath("$.components.schemas.CreateBranchRequest.properties.status.enum")
                        .value(hasItems("OPEN", "TEMPORARILY_CLOSED", "CLOSED")));
    }

    @Test
    void documentsTheBranchRename() throws Exception {
        String rename = "$.paths['/api/v1/franchises/{franchiseId}/branches/{branchId}/name'].patch";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(rename + ".tags[0]").value("Sucursales"))
                .andExpect(jsonPath(rename + ".requestBody.content['application/json'].schema.$ref")
                        .value("#/components/schemas/UpdateNameRequest"))
                .andExpect(jsonPath(rename + ".responses['200'].content['application/json'].schema.$ref")
                        .value("#/components/schemas/BranchResponse"))
                .andExpect(jsonPath(rename + ".responses['404'].$ref").value("#/components/responses/NotFound"))
                .andExpect(jsonPath(rename + ".responses['409'].$ref").value("#/components/responses/Conflict"));
    }

    @Test
    void documentsTheProductEndpoints() throws Exception {
        String products = "$.paths['/api/v1/franchises/{franchiseId}/branches/{branchId}/products";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(products + "'].post.tags[0]").value("Productos"))
                .andExpect(jsonPath(products + "'].post.responses['201'].headers.Location").exists())
                .andExpect(jsonPath(products + "/{productId}'].delete.responses['204'].description")
                        .value("Producto eliminado"))
                .andExpect(jsonPath(products + "/{productId}/stock'].patch.responses['200']").exists())
                .andExpect(jsonPath(products + "/{productId}/stock'].patch.responses['400'].$ref")
                        .value("#/components/responses/BadRequest"))
                .andExpect(jsonPath("$.components.schemas.CreateProductRequest.required")
                        .value(hasItems("name", "price", "stock")))
                .andExpect(jsonPath("$.components.schemas.ProductResponse.properties.lowStock.type").value("boolean"));
    }

    @Test
    void documentsTheTopStockReport() throws Exception {
        String report = "$.paths['/api/v1/franchises/{franchiseId}/top-stock-products'].get";
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(jsonPath(report + ".tags[0]").value("Franquicias"))
                .andExpect(jsonPath(report + ".responses['200'].content['application/json'].schema.items.$ref")
                        .value("#/components/schemas/TopStockProductResponse"))
                .andExpect(jsonPath(report + ".responses['404'].$ref").value("#/components/responses/NotFound"));
    }

    @Test
    void servesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

}
