package com.example.franchisesapi.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GlobalExceptionHandlerTest.FailingController.class)
@Import(GlobalExceptionHandlerTest.FailingController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void notFoundReturns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Franchise 'f1' not found"));
    }

    @Test
    void duplicatesReturn409() throws Exception {
        mockMvc.perform(get("/test/duplicate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Branch 'Centro' already exists"));
        mockMvc.perform(get("/test/duplicate-key"))
                .andExpect(status().isConflict());
    }

    @Test
    void businessRuleViolationReturns422() throws Exception {
        mockMvc.perform(get("/test/business-rule"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Branch 'Centro' is closed"));
    }

    @Test
    void concurrentModificationReturns409() throws Exception {
        mockMvc.perform(get("/test/concurrent"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("The franchise was modified by another request, please retry"));
    }

    @Test
    void invalidBodyReturns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " ", "stock": -1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Request validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.stock").exists());
    }

    @Test
    void malformedJsonReturns400ProblemDetail() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    record SampleRequest(@NotBlank String name, @PositiveOrZero int stock) {
    }

    @RestController
    static class FailingController {

        @GetMapping("/test/{scenario}")
        void fail(@PathVariable String scenario) {
            throw switch (scenario) {
                case "not-found" -> ResourceNotFoundException.franchise("f1");
                case "duplicate" -> new DuplicateResourceException("Branch 'Centro' already exists");
                case "duplicate-key" -> new DuplicateKeyException("E11000 duplicate key");
                case "business-rule" -> new BusinessRuleException("Branch 'Centro' is closed");
                case "concurrent" -> new OptimisticLockingFailureException("stale version");
                default -> new IllegalArgumentException(scenario);
            };
        }

        @PostMapping("/test/validate")
        void validate(@Valid @RequestBody SampleRequest request) {
        }

    }

}
