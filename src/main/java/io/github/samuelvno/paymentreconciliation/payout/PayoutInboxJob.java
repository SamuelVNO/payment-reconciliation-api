package io.github.samuelvno.paymentreconciliation.payout;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Simula o adquirente depositando arquivos por SFTP: lê {inbox}/{storeId}/*.csv,
 * importa cada arquivo e o move para processed/ ou failed/.
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class PayoutInboxJob {

    private static final Logger log = LoggerFactory.getLogger(PayoutInboxJob.class);

    private final PayoutImportService importService;
    private final PayoutInboxProperties properties;

    public PayoutInboxJob(PayoutImportService importService, PayoutInboxProperties properties) {
        this.importService = importService;
        this.properties = properties;
    }

    @Scheduled(cron = "${app.payout-inbox.cron}")
    @SchedulerLock(name = "payout-inbox-import", lockAtLeastFor = "PT30S")
    public void run() {
        int files = processInbox();
        if (files > 0) {
            log.info("Payout inbox job processed {} file(s)", files);
        }
    }

    int processInbox() {
        Path root = properties.directory();
        if (!Files.isDirectory(root)) {
            log.debug("Payout inbox {} does not exist, skipping", root);
            return 0;
        }
        int processed = 0;
        for (Path storeDir : list(root, Files::isDirectory)) {
            Optional<UUID> storeId = parseStoreId(storeDir);
            if (storeId.isEmpty()) {
                log.warn("Ignoring inbox folder {}: name is not a store id", storeDir.getFileName());
                continue;
            }
            for (Path file : list(storeDir, path -> Files.isRegularFile(path) && path.toString().endsWith(".csv"))) {
                processFile(storeId.get(), storeDir, file);
                processed++;
            }
        }
        return processed;
    }

    private void processFile(UUID storeId, Path storeDir, Path file) {
        try {
            byte[] content = Files.readAllBytes(file);
            // Chave derivada do conteúdo: o mesmo arquivo depositado duas vezes não é reimportado.
            String idempotencyKey = "sha256:" + sha256(content);
            PayoutImportService.ImportOutcome outcome = importService.importCsv(
                    storeId, idempotencyKey, file.getFileName().toString(), new ByteArrayInputStream(content));
            log.info("Inbox file {} for store {} -> import {} (replayed={})",
                    file.getFileName(), storeId, outcome.result().id(), outcome.replayed());
            moveTo(storeDir.resolve("processed"), file);
        } catch (IOException | RuntimeException ex) {
            // Um arquivo ruim não pode travar a fila: vai para failed/ e o job segue.
            log.error("Failed to import inbox file {} for store {}", file.getFileName(), storeId, ex);
            moveTo(storeDir.resolve("failed"), file);
        }
    }

    private static Optional<UUID> parseStoreId(Path dir) {
        try {
            return Optional.of(UUID.fromString(dir.getFileName().toString()));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    private static List<Path> list(Path dir, Predicate<Path> filter) {
        try (Stream<Path> paths = Files.list(dir)) {
            return paths.filter(filter).sorted().toList();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static void moveTo(Path targetDir, Path file) {
        try {
            Files.createDirectories(targetDir);
            Files.move(file, targetDir.resolve(file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is always available in the JDK", ex);
        }
    }
}
