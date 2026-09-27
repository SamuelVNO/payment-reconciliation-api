package io.github.samuelvno.paymentreconciliation.store;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.samuelvno.paymentreconciliation.shared.error.ResourceNotFoundException;
import io.github.samuelvno.paymentreconciliation.shared.security.SecurityConfig;

import static io.github.samuelvno.paymentreconciliation.TestAuth.admin;
import static io.github.samuelvno.paymentreconciliation.TestAuth.storeUser;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Fatia só a camada web: controller + advice + Jackson + validação + segurança. Sem banco, sem service real.
@WebMvcTest(StoreController.class)
@Import(SecurityConfig.class)
class StoreControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreService storeService;

    @Test
    void createsStoreAndReturnsLocation() throws Exception {
        UUID id = UUID.randomUUID();
        when(storeService.create(any())).thenReturn(
                new StoreResponse(id, "Loja", "12345678000190", new BigDecimal("0.0299"), Instant.now()));

        mockMvc.perform(post("/api/stores").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Loja", "document": "12345678000190", "feeRate": 0.0299}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/stores/" + id))
                .andExpect(jsonPath("$.feeRate").value(0.0299));
    }

    @Test
    void returnsProblemDetailWithFieldErrors() throws Exception {
        mockMvc.perform(post("/api/stores").with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "", "document": "123", "feeRate": 2}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Content-Type", MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Invalid request"))
                .andExpect(jsonPath("$.errors", hasSize(3)))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')].message").value("must not be blank"));
    }

    @Test
    void returns404AsProblemDetail() throws Exception {
        UUID id = UUID.randomUUID();
        when(storeService.findById(id)).thenThrow(new ResourceNotFoundException("Store", id));

        mockMvc.perform(get("/api/stores/{id}", id).with(admin()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Store %s not found".formatted(id)));
    }

    @Test
    void storeUserCannotCreateStores() throws Exception {
        mockMvc.perform(post("/api/stores").with(storeUser(UUID.randomUUID()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Loja", "document": "12345678000190", "feeRate": 0.0299}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
    }

    @Test
    void storeUserCannotReadAnotherStore() throws Exception {
        mockMvc.perform(get("/api/stores/{id}", UUID.randomUUID()).with(storeUser(UUID.randomUUID())))
                .andExpect(status().isForbidden());
    }

    @Test
    void requestWithoutTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/stores/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
    }
}
