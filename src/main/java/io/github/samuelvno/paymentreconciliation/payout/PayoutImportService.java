package io.github.samuelvno.paymentreconciliation.payout;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.samuelvno.paymentreconciliation.shared.error.ResourceNotFoundException;
import io.github.samuelvno.paymentreconciliation.store.Store;
import io.github.samuelvno.paymentreconciliation.store.StoreService;

@Service
@Transactional(readOnly = true)
public class PayoutImportService {

    private static final Logger log = LoggerFactory.getLogger(PayoutImportService.class);

    private final PayoutImportRepository importRepository;
    private final PayoutRepository payoutRepository;
    private final PayoutCsvParser parser;
    private final StoreService storeService;

    public PayoutImportService(PayoutImportRepository importRepository, PayoutRepository payoutRepository,
                               PayoutCsvParser parser, StoreService storeService) {
        this.importRepository = importRepository;
        this.payoutRepository = payoutRepository;
        this.parser = parser;
        this.storeService = storeService;
    }

    public record ImportOutcome(PayoutImportResponse result, boolean replayed) {
    }

    /**
     * Idempotência em dois níveis:
     * 1. Idempotency-Key: reenviar a mesma requisição devolve o resultado original, sem reprocessar.
     * 2. payout_id: um repasse já importado por outro arquivo é contado como duplicado e ignorado.
     */
    @Transactional
    public ImportOutcome importCsv(UUID storeId, String idempotencyKey, String fileName, InputStream content) {
        Optional<PayoutImport> previous = importRepository.findByStoreIdAndIdempotencyKey(storeId, idempotencyKey);
        if (previous.isPresent()) {
            log.info("Replaying payout import {} for key {}", previous.get().getId(), idempotencyKey);
            return new ImportOutcome(PayoutImportResponse.from(previous.get()), true);
        }

        Store store = storeService.getStore(storeId);
        PayoutCsvParser.ParseResult parsed = parser.parse(content);
        PayoutImport payoutImport = importRepository.save(new PayoutImport(store, idempotencyKey, fileName));

        Set<String> existing = parsed.rows().isEmpty()
                ? Set.of()
                : payoutRepository.findExistingExternalIds(storeId,
                        parsed.rows().stream().map(PayoutCsvParser.PayoutRow::externalId).toList());

        Set<String> seenInFile = new HashSet<>();
        List<Payout> toInsert = new ArrayList<>();
        int duplicates = 0;
        for (PayoutCsvParser.PayoutRow row : parsed.rows()) {
            // Set.add devolve false se o id já apareceu antes neste mesmo arquivo
            if (existing.contains(row.externalId()) || !seenInFile.add(row.externalId())) {
                duplicates++;
            } else {
                toInsert.add(new Payout(store, payoutImport, row));
            }
        }
        payoutRepository.saveAll(toInsert);
        payoutImport.summarize(parsed.totalRows(), toInsert.size(), duplicates, parsed.errors());

        log.info("Payout import {} finished: total={} imported={} duplicates={} rejected={}",
                payoutImport.getId(), parsed.totalRows(), toInsert.size(), duplicates, parsed.errors().size());
        return new ImportOutcome(PayoutImportResponse.from(payoutImport), false);
    }

    public PayoutImportResponse findImport(UUID storeId, UUID importId) {
        return importRepository.findByIdAndStoreId(importId, storeId)
                .map(PayoutImportResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Payout import", importId));
    }

    public Page<PayoutImportResponse> findImports(UUID storeId, Pageable pageable) {
        return importRepository.findByStoreId(storeId, pageable).map(PayoutImportResponse::from);
    }

    public Page<PayoutResponse> findPayouts(UUID storeId, Pageable pageable) {
        return payoutRepository.findByStoreId(storeId, pageable).map(PayoutResponse::from);
    }
}
