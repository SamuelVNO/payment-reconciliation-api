package io.github.samuelvno.paymentreconciliation.shared;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.actuate.observability.AutoConfigureObservability;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import io.github.samuelvno.paymentreconciliation.TestcontainersConfiguration;

import static io.github.samuelvno.paymentreconciliation.TestAuth.admin;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Em teste o Boot desliga a exportação de métricas (e o /actuator/prometheus some): aqui religamos.
@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureObservability(tracing = false)
@Import(TestcontainersConfiguration.class)
class ObservabilityIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void probesArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void metricsRequireAdmin() throws Exception {
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/actuator/prometheus").with(admin()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("outbox_pending")))
                .andExpect(content().string(containsString("reconciliation_run_duration_seconds")));
    }

    @Test
    void openApiDocumentsBearerAuth() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Payment Reconciliation API"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
    }

    @Test
    void everyResponseCarriesRequestId() throws Exception {
        mockMvc.perform(get("/ping"))
                .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")));
        mockMvc.perform(get("/ping").header("X-Request-Id", "gateway-req-12345"))
                .andExpect(header().string("X-Request-Id", "gateway-req-12345"));
        // id com caracteres perigosos (quebra de linha forjaria linhas de log) é descartado
        mockMvc.perform(get("/ping").header("X-Request-Id", "abc\nFAKE LOG LINE"))
                .andExpect(header().string("X-Request-Id", matchesPattern("[0-9a-f-]{36}")));
    }
}
