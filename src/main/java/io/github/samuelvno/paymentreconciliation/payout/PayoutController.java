package io.github.samuelvno.paymentreconciliation.payout;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.github.samuelvno.paymentreconciliation.shared.error.InvalidRequestException;

@RestController
@RequestMapping("/api/stores/{storeId}")
public class PayoutController {

    private final PayoutImportService importService;

    public PayoutController(PayoutImportService importService) {
        this.importService = importService;
    }

    @PostMapping(path = "/payout-imports", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PayoutImportResponse> importCsv(
            @PathVariable UUID storeId,
            @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 100) String idempotencyKey,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new InvalidRequestException("File is empty");
        }
        String fileName = file.getOriginalFilename() == null ? "payouts.csv" : file.getOriginalFilename();
        PayoutImportService.ImportOutcome outcome =
                importService.importCsv(storeId, idempotencyKey, fileName, file.getInputStream());

        if (outcome.replayed()) {
            return ResponseEntity.ok().header("Idempotent-Replayed", "true").body(outcome.result());
        }
        URI location = URI.create("/api/stores/%s/payout-imports/%s".formatted(storeId, outcome.result().id()));
        return ResponseEntity.created(location).body(outcome.result());
    }

    @GetMapping("/payout-imports/{importId}")
    public PayoutImportResponse findImport(@PathVariable UUID storeId, @PathVariable UUID importId) {
        return importService.findImport(storeId, importId);
    }

    @GetMapping("/payouts")
    public Page<PayoutResponse> findPayouts(@PathVariable UUID storeId,
                                            @PageableDefault(size = 20, sort = "paidAt", direction = Sort.Direction.DESC)
                                            Pageable pageable) {
        return importService.findPayouts(storeId, pageable);
    }
}
