package io.github.samuelvno.paymentreconciliation.store;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * CNPJs falsos e únicos para testes de integração.
 * Não use System.nanoTime(): a origem é arbitrária (na prática, o uptime da máquina),
 * então numa VM de CI recém-criada ele tem menos de 14 dígitos.
 */
public final class TestDocuments {

    private static final AtomicLong NEXT =
            new AtomicLong(ThreadLocalRandom.current().nextLong(10_000_000_000_000L, 90_000_000_000_000L));

    private TestDocuments() {
    }

    public static String uniqueCnpj() {
        return String.valueOf(NEXT.getAndIncrement());
    }
}
