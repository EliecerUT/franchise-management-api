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
    void servesSwaggerUi() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

}
