package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import io.github.samuelvno.paymentreconciliation.TestcontainersConfiguration;
import io.github.samuelvno.paymentreconciliation.order.Order;
import io.github.samuelvno.paymentreconciliation.order.OrderRepository;
import io.github.samuelvno.paymentreconciliation.payout.PayoutImportService;
import io.github.samuelvno.paymentreconciliation.store.Store;
import io.github.samuelvno.paymentreconciliation.store.StoreRepository;

import static io.github.samuelvno.paymentreconciliation.TestAuth.storeUser;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ReconciliationIT {

    // "Hoje" congelado: as regras de prazo (D+2) dão sempre o mesmo resultado.
    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(LocalDate.of(2026, 9, 27).atTime(10, 0)
                    .atZone(ZoneId.of("America/Sao_Paulo")).toInstant(), ZoneId.of("America/Sao_Paulo"));
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PayoutImportService payoutImportService;

    private UUID storeId;

    @BeforeEach
    void setUp() throws Exception {
        String document = String.valueOf(System.nanoTime()).substring(0, 14);
        Store store = storeRepository.save(new Store("Loja IT", document, new BigDecimal("0.0299")));
        storeId = store.getId();
        order(store, "PED-1001", "150.00", "2026-09-20");
        order(store, "PED-1002", "89.90", "2026-09-20");
        order(store, "PED-1003", "200.00", "2026-09-21");
        order(store, "PED-1004", "75.00", "2026-09-21");
        order(store, "PED-1005", "120.00", "2026-09-22");
        order(store, "PED-1006", "60.00", "2026-09-26");
        byte[] csv = Files.readAllBytes(Path.of("samples/payouts-2026-09.csv"));
        payoutImportService.importCsv(storeId, "it-" + storeId, "payouts.csv", new ByteArrayInputStream(csv));
    }

    @Test
    void detectsEveryDiscrepancyTypeAndIsIdempotent() throws Exception {
        mockMvc.perform(post("/api/stores/{storeId}/reconciliation-runs", storeId).with(storeUser(storeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ordersChecked").value(6))
                .andExpect(jsonPath("$.reconciled").value(1))
                .andExpect(jsonPath("$.divergent").value(4))
                .andExpect(jsonPath("$.stillPending").value(1))
                .andExpect(jsonPath("$.discrepanciesFound").value(5));

        mockMvc.perform(get("/api/stores/{storeId}/discrepancies", storeId).with(storeUser(storeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").value(5))
                .andExpect(jsonPath("$.content[*].type", containsInAnyOrder(
                        "FEE_OVERCHARGED", "AMOUNT_MISMATCH", "MISSING_PAYOUT", "DUPLICATE_PAYOUT", "ORPHAN_PAYOUT")));

        // Segunda execução: nada novo para conciliar, nenhuma divergência duplicada.
        mockMvc.perform(post("/api/stores/{storeId}/reconciliation-runs", storeId).with(storeUser(storeId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ordersChecked").value(1))
                .andExpect(jsonPath("$.discrepanciesFound").value(0));
    }

    @Test
    void filtersResolvesAndReports() throws Exception {
        mockMvc.perform(post("/api/stores/{storeId}/reconciliation-runs", storeId).with(storeUser(storeId))).andExpect(status().isCreated());

        String body = mockMvc.perform(get("/api/stores/{storeId}/discrepancies", storeId).with(storeUser(storeId)).param("type", "FEE_OVERCHARGED"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].orderExternalId").value("PED-1002"))
                .andExpect(jsonPath("$.content[0].difference").value(0.81))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.content[0].id");

        mockMvc.perform(patch("/api/stores/{storeId}/discrepancies/{id}/resolve", storeId, id).with(storeUser(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": \"Adquirente estornou a diferença\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/stores/{storeId}/discrepancies/{id}/resolve", storeId, id).with(storeUser(storeId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"note\": \"de novo\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/stores/{storeId}/reconciliation-report", storeId).with(storeUser(storeId))
                        .param("from", "2026-09-01").param("to", "2026-09-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ordersByStatus.RECONCILED").value(1))
                .andExpect(jsonPath("$.ordersByStatus.DIVERGENT").value(4))
                .andExpect(jsonPath("$.ordersByStatus.PENDING").value(1))
                .andExpect(jsonPath("$.discrepanciesByType.length()").value(5))
                .andExpect(jsonPath("$.openDiscrepancies").value(4));
    }

    @Test
    void orphanPayoutIsResolvedWhenOrderIsRegisteredLater() throws Exception {
        mockMvc.perform(post("/api/stores/{storeId}/reconciliation-runs", storeId).with(storeUser(storeId))).andExpect(status().isCreated());
        order(storeRepository.findById(storeId).orElseThrow(), "PED-9999", "50.00", "2026-09-23");

        mockMvc.perform(post("/api/stores/{storeId}/reconciliation-runs", storeId).with(storeUser(storeId)))
                .andExpect(jsonPath("$.reconciled").value(1));

        mockMvc.perform(get("/api/stores/{storeId}/discrepancies", storeId).with(storeUser(storeId))
                        .param("type", "ORPHAN_PAYOUT").param("status", "RESOLVED"))
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    private void order(Store store, String externalId, String amount, String date) {
        orderRepository.save(new Order(store, externalId, new BigDecimal(amount), LocalDate.parse(date)));
    }
}
