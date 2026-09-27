package io.github.samuelvno.paymentreconciliation;

import java.util.UUID;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Injeta um JWT já "validado" no MockMvc, sem assinar token de verdade.
 * O fluxo real (login -> token -> requisição) é coberto pelo SecurityIT.
 */
public final class TestAuth {

    private TestAuth() {
    }

    public static RequestPostProcessor admin() {
        return jwt().jwt(token -> token.claim("role", "ADMIN"))
                .authorities(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    public static RequestPostProcessor storeUser(UUID storeId) {
        return jwt().jwt(token -> token.claim("role", "STORE_USER").claim("store_id", storeId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_STORE_USER"));
    }
}
