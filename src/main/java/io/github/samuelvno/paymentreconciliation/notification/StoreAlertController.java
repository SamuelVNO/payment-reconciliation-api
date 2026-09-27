package io.github.samuelvno.paymentreconciliation.notification;

import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Alertas", description = "Alertas gerados pelo consumidor Kafka de divergências.")
public class StoreAlertController {

    private final StoreAlertService alertService;

    public StoreAlertController(StoreAlertService alertService) {
        this.alertService = alertService;
    }

    @Operation(summary = "Lista alertas da loja")
    @GetMapping("/api/stores/{storeId}/alerts")
    public Page<StoreAlertResponse> alerts(@PathVariable UUID storeId,
                                           @PageableDefault(size = 20, sort = "createdAt",
                                                   direction = Sort.Direction.DESC) Pageable pageable) {
        return alertService.findByStore(storeId, pageable);
    }
}
