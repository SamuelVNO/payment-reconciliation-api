package io.github.samuelvno.paymentreconciliation.payout;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import io.github.samuelvno.paymentreconciliation.TestcontainersConfiguration;
import io.github.samuelvno.paymentreconciliation.store.Store;
import io.github.samuelvno.paymentreconciliation.store.StoreRepository;

import static io.github.samuelvno.paymentreconciliation.TestAuth.admin;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PayoutImportIT {

    private static final String CSV = """
            payout_id,order_id,gross_amount,fee_amount,net_amount,paid_at
            PAY-1,PED-1,150.00,4.48,145.52,2026-09-22
            PAY-2,PED-2,89.90,2.69,87.21,2026-09-22
            PAY-1,PED-1,150.00,4.48,145.52,2026-09-22
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private PayoutRepository payoutRepository;

    @Test
    void sameIdempotencyKeyReplaysResultWithoutReimporting() throws Exception {
        Store store = storeRepository.save(new Store("Loja IT", "33333333000133", new BigDecimal("0.0299")));
        MockMultipartFile file = new MockMultipartFile("file", "payouts.csv", "text/csv",
                CSV.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/stores/{storeId}/payout-imports", store.getId())
                        .file(file).header("Idempotency-Key", "key-1").with(admin()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.importedRows").value(2))
                .andExpect(jsonPath("$.duplicateRows").value(1));

        mockMvc.perform(multipart("/api/stores/{storeId}/payout-imports", store.getId())
                        .file(file).header("Idempotency-Key", "key-1").with(admin()))
                .andExpect(status().isOk())
                .andExpect(header().string("Idempotent-Replayed", "true"))
                .andExpect(jsonPath("$.importedRows").value(2));

        assertThat(payoutRepository.findByStoreId(store.getId(), Pageable.unpaged())
                .getTotalElements()).isEqualTo(2);
    }
}
