package io.github.samuelvno.paymentreconciliation.payout;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.samuelvno.paymentreconciliation.shared.error.ResourceNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PayoutInboxJobTest {

    private static final String CSV = """
            payout_id,order_id,gross_amount,fee_amount,net_amount,paid_at
            PAY-1,PED-1,150.00,4.48,145.52,2026-09-22
            """;

    @TempDir
    private Path inbox;

    @Mock
    private PayoutImportService importService;

    private PayoutInboxJob job;

    @BeforeEach
    void setUp() {
        job = new PayoutInboxJob(importService, new PayoutInboxProperties(inbox, "0 */5 * * * *"));
    }

    @Test
    void importsFileWithContentHashAsKeyAndMovesItToProcessed() throws Exception {
        UUID storeId = UUID.randomUUID();
        Path file = write(storeId, "payouts.csv");
        when(importService.importCsv(eq(storeId), startsWith("sha256:"), eq("payouts.csv"), any(InputStream.class)))
                .thenReturn(new PayoutImportService.ImportOutcome(response(), false));

        int processed = job.processInbox();

        assertThat(processed).isEqualTo(1);
        assertThat(file).doesNotExist();
        assertThat(inbox.resolve(storeId + "/processed/payouts.csv")).exists();
    }

    @Test
    void movesFileToFailedWhenImportThrows() throws Exception {
        UUID storeId = UUID.randomUUID();
        write(storeId, "payouts.csv");
        when(importService.importCsv(eq(storeId), any(), any(), any()))
                .thenThrow(new ResourceNotFoundException("Store", storeId));

        job.processInbox();

        assertThat(inbox.resolve(storeId + "/failed/payouts.csv")).exists();
    }

    @Test
    void ignoresFoldersThatAreNotStoreIds() throws Exception {
        Files.createDirectories(inbox.resolve("not-a-uuid"));
        Files.writeString(inbox.resolve("not-a-uuid/payouts.csv"), CSV);

        assertThat(job.processInbox()).isZero();
        verifyNoInteractions(importService);
    }

    @Test
    void sameContentProducesSameIdempotencyKey() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(importService.importCsv(eq(storeId), any(), any(), any()))
                .thenReturn(new PayoutImportService.ImportOutcome(response(), false));

        write(storeId, "a.csv");
        job.processInbox();
        write(storeId, "b.csv");
        job.processInbox();

        ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
        verify(importService, times(2)).importCsv(eq(storeId), keys.capture(), any(), any());
        assertThat(keys.getAllValues().get(0)).isEqualTo(keys.getAllValues().get(1));
    }

    private Path write(UUID storeId, String name) throws Exception {
        Path dir = Files.createDirectories(inbox.resolve(storeId.toString()));
        return Files.writeString(dir.resolve(name), CSV, StandardCharsets.UTF_8);
    }

    private static PayoutImportResponse response() {
        return new PayoutImportResponse(UUID.randomUUID(), "k", "payouts.csv", 1, 1, 0, 0, List.of(), Instant.now());
    }
}
