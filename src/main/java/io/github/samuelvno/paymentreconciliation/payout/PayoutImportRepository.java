package io.github.samuelvno.paymentreconciliation.payout;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PayoutImportRepository extends JpaRepository<PayoutImport, UUID> {

    Optional<PayoutImport> findByStoreIdAndIdempotencyKey(UUID storeId, String idempotencyKey);

    Optional<PayoutImport> findByIdAndStoreId(UUID id, UUID storeId);

    Page<PayoutImport> findByStoreId(UUID storeId, Pageable pageable);
}
