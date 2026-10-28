package com.example.franchisesapi.controller;

import com.example.franchisesapi.dto.AddressRequest;
import com.example.franchisesapi.dto.CreateBranchRequest;
import com.example.franchisesapi.exception.BusinessRuleException;
import com.example.franchisesapi.exception.DuplicateResourceException;
import com.example.franchisesapi.exception.ResourceNotFoundException;
import com.example.franchisesapi.model.Address;
import com.example.franchisesapi.model.Branch;
import com.example.franchisesapi.model.BranchStatus;
import com.example.franchisesapi.service.BranchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BranchController.class)
class BranchControllerTest {

    private static final String BRANCHES_URL = "/api/v1/franchises/f1/branches";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private BranchService branchService;

    @Test
    void addBranchReturns201WithLocationAndAllTheFields() throws Exception {
        CreateBranchRequest request = new CreateBranchRequest("Centro", "BOG-001",
                new AddressRequest("Cra 7 # 12-34", "Bogotá", "Cundinamarca", "Colombia", "110311"),
                "+57 601 555 0001", "centro@burgerexpress.com", "Laura Gómez", LocalDate.of(2020, 3, 15), null);
        when(branchService.addBranch("f1", request)).thenReturn(Branch.builder()
                .id("b1").code("BOG-001").name("Centro")
                .address(Address.builder().street("Cra 7 # 12-34").city("Bogotá").state("Cundinamarca")
                        .country("Colombia").postalCode("110311").build())
                .phone("+57 601 555 0001").email("centro@burgerexpress.com").managerName("Laura Gómez")
                .openingDate(LocalDate.of(2020, 3, 15))
                .build());

        mockMvc.perform(post(BRANCHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Centro",
                                  "code": "BOG-001",
                                  "address": {
                                    "street": "Cra 7 # 12-34",
                                    "city": "Bogotá",
                                    "state": "Cundinamarca",
                                    "country": "Colombia",
                                    "postalCode": "110311"
                                  },
                                  "phone": "+57 601 555 0001",
                                  "email": "centro@burgerexpress.com",
                                  "managerName": "Laura Gómez",
                                  "openingDate": "2020-03-15"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + BRANCHES_URL + "/b1"))
                .andExpect(jsonPath("$.id").value("b1"))
                .andExpect(jsonPath("$.code").value("BOG-001"))
                .andExpect(jsonPath("$.address.city").value("Bogotá"))
                .andExpect(jsonPath("$.managerName").value("Laura Gómez"))
                .andExpect(jsonPath("$.openingDate").value("2020-03-15"))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.products").isEmpty());
    }

    @Test
    void addBranchWithInvalidFieldsReturns400WithEveryError() throws Exception {
        mockMvc.perform(post(BRANCHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "  ",
                                  "code": "BOG 001!",
                                  "address": {"street": "Cra 7"},
                                  "email": "centro",
                                  "openingDate": "2999-01-01"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.code").value("must contain 2 to 30 letters, digits or hyphens"))
                .andExpect(jsonPath("$.errors['address.city']").exists())
                .andExpect(jsonPath("$.errors['address.country']").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.openingDate").exists());
        verifyNoInteractions(branchService);
    }

    @Test
    void addBranchWithUnknownStatusReturns400() throws Exception {
        mockMvc.perform(post(BRANCHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Centro", "status": "DEMOLISHED"}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(branchService);
    }

    @Test
    void addBranchToUnknownFranchiseReturns404() throws Exception {
        when(branchService.addBranch(eq("missing"), any())).thenThrow(ResourceNotFoundException.franchise("missing"));

        mockMvc.perform(post("/api/v1/franchises/missing/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Centro"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Franchise 'missing' not found"));
    }

    @Test
    void addDuplicatedBranchReturns409() throws Exception {
        when(branchService.addBranch(eq("f1"), any()))
                .thenThrow(new DuplicateResourceException("Branch 'Centro' already exists in franchise 'Burger'"));

        mockMvc.perform(post(BRANCHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Centro"}
                                """))
                .andExpect(status().isConflict());
    }

    @Test
    void addBranchToInactiveFranchiseReturns422() throws Exception {
        when(branchService.addBranch(eq("f1"), any()))
                .thenThrow(new BusinessRuleException("Franchise 'Burger' is inactive and does not accept new branches"));

        mockMvc.perform(post(BRANCHES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Centro"}
                                """))
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void getByIdReturnsTheBranch() throws Exception {
        when(branchService.getById("f1", "b1")).thenReturn(Branch.builder()
                .id("b1").name("Centro").status(BranchStatus.TEMPORARILY_CLOSED).build());

        mockMvc.perform(get(BRANCHES_URL + "/b1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Centro"))
                .andExpect(jsonPath("$.status").value("TEMPORARILY_CLOSED"));
    }

    @Test
    void updateNameReturns200WithTheRenamedBranch() throws Exception {
        when(branchService.updateName("f1", "b1", "Centro Histórico"))
                .thenReturn(Branch.builder().id("b1").name("Centro Histórico").build());

        mockMvc.perform(patch(BRANCHES_URL + "/b1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Centro Histórico"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("b1"))
                .andExpect(jsonPath("$.name").value("Centro Histórico"));
    }

    @Test
    void updateNameWithBlankNameReturns400() throws Exception {
        mockMvc.perform(patch(BRANCHES_URL + "/b1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": ""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
        verifyNoInteractions(branchService);
    }

    @Test
    void updateNameOfUnknownBranchReturns404() throws Exception {
        when(branchService.updateName("f1", "missing", "Centro")).thenThrow(ResourceNotFoundException.branch("missing"));

        mockMvc.perform(patch(BRANCHES_URL + "/missing/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Centro"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateNameToAnExistingNameReturns409() throws Exception {
        when(branchService.updateName("f1", "b1", "Norte"))
                .thenThrow(new DuplicateResourceException("Branch 'Norte' already exists in franchise 'Burger'"));

        mockMvc.perform(patch(BRANCHES_URL + "/b1/name")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Norte"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Branch 'Norte' already exists in franchise 'Burger'"));
    }

}
