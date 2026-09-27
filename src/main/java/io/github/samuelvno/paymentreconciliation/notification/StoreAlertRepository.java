package io.github.samuelvno.paymentreconciliation.notification;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreAlertRepository extends JpaRepository<StoreAlert, UUID> {

    boolean existsByEventId(UUID eventId);

    Page<StoreAlert> findByStoreId(UUID storeId, Pageable pageable);

    long countByStoreId(UUID storeId);
}
