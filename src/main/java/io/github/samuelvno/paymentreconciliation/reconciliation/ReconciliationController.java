package io.github.samuelvno.paymentreconciliation.reconciliation;

import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Conciliação", description = "Motor de conciliação, divergências e relatório.")
@RequestMapping("/api/stores/{storeId}")
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Operation(summary = "Executa a conciliação da loja agora")
    @PostMapping("/reconciliation-runs")
    public ResponseEntity<ReconciliationRunResponse> run(@PathVariable UUID storeId) {
        ReconciliationRunResponse run = reconciliationService.reconcile(storeId, TriggerSource.MANUAL);
        return ResponseEntity.created(URI.create("/api/stores/%s/reconciliation-runs/%s".formatted(storeId, run.id())))
                .body(run);
    }

    @Operation(summary = "Histórico de execuções")
    @GetMapping("/reconciliation-runs")
    public Page<ReconciliationRunResponse> runs(@PathVariable UUID storeId,
                                                @PageableDefault(size = 20, sort = "createdAt",
                                                        direction = Sort.Direction.DESC) Pageable pageable) {
        return reconciliationService.findRuns(storeId, pageable);
    }

    @Operation(summary = "Lista divergências com filtros por tipo e status")
    @GetMapping("/discrepancies")
    public Page<DiscrepancyView> discrepancies(@PathVariable UUID storeId,
                                               @RequestParam(required = false) DiscrepancyType type,
                                               @RequestParam(required = false) DiscrepancyStatus status,
                                               @PageableDefault(size = 20, sort = "createdAt",
                                                       direction = Sort.Direction.DESC) Pageable pageable) {
        return reconciliationService.findDiscrepancies(storeId, type, status, pageable);
    }

    @Operation(summary = "Marca uma divergência como resolvida")
    @PatchMapping("/discrepancies/{discrepancyId}/resolve")
    public ResponseEntity<Void> resolve(@PathVariable UUID storeId, @PathVariable UUID discrepancyId,
                                        @Valid @RequestBody ResolveDiscrepancyRequest request) {
        reconciliationService.resolve(storeId, discrepancyId, request.note());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Relatório agregado do período")
    @GetMapping("/reconciliation-report")
    public ReconciliationReport report(@PathVariable UUID storeId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return reconciliationService.report(storeId, from, to);
    }
}
