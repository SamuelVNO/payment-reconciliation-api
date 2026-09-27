package io.github.samuelvno.paymentreconciliation.shared.security;

import java.util.function.Supplier;

import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/**
 * Isolamento multi-tenant na borda: para /api/stores/{storeId}/**, o {storeId} da URL
 * precisa ser igual à claim store_id do token. ADMIN passa direto.
 * Os repositories também filtram por storeId (defesa em profundidade).
 */
public class TenantAuthorizationManager implements AuthorizationManager<RequestAuthorizationContext> {

    @Override
    public AuthorizationDecision check(Supplier<Authentication> authentication, RequestAuthorizationContext context) {
        if (!(authentication.get() instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            return new AuthorizationDecision(false);
        }
        boolean admin = token.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (admin) {
            return new AuthorizationDecision(true);
        }
        String pathStoreId = context.getVariables().get("storeId");
        String tokenStoreId = token.getToken().getClaimAsString(TenantClaims.STORE_ID);
        return new AuthorizationDecision(pathStoreId != null && pathStoreId.equalsIgnoreCase(tokenStoreId));
    }
}
