package io.github.samuelvno.paymentreconciliation.shared.security;

/** Nomes das claims customizadas que a API coloca no JWT. */
public final class TenantClaims {

    public static final String EMAIL = "email";
    public static final String ROLE = "role";
    public static final String STORE_ID = "store_id";

    private TenantClaims() {
    }
}
