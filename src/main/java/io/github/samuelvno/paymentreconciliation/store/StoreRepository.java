package io.github.samuelvno.paymentreconciliation.store;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    boolean existsByDocument(String document);

    @Query("select s.id from Store s order by s.id")
    List<UUID> findAllIds();
}
