package io.github.samuelvno.paymentreconciliation.payout;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PayoutRepository extends JpaRepository<Payout, UUID> {

    // Busca só os ids que já existem, em uma query, em vez de um SELECT por linha do CSV.
    @Query("select p.externalId from Payout p where p.store.id = :storeId and p.externalId in :externalIds")
    Set<String> findExistingExternalIds(@Param("storeId") UUID storeId,
                                        @Param("externalIds") Collection<String> externalIds);

    Page<Payout> findByStoreId(UUID storeId, Pageable pageable);
}
