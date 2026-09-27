package io.github.samuelvno.paymentreconciliation.auth;

public enum Role {
    /** Operador da plataforma: vê e administra todas as lojas. */
    ADMIN,
    /** Usuário de uma loja: só enxerga os dados da própria loja. */
    STORE_USER
}
