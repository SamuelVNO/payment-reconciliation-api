package io.github.samuelvno.paymentreconciliation.auth;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.samuelvno.paymentreconciliation.shared.security.TenantClaims;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/auth/me")
    public Map<String, Object> me(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> me = new LinkedHashMap<>();
        me.put("userId", jwt.getSubject());
        me.put("email", jwt.getClaimAsString(TenantClaims.EMAIL));
        me.put("role", jwt.getClaimAsString(TenantClaims.ROLE));
        me.put("storeId", jwt.getClaimAsString(TenantClaims.STORE_ID));
        me.put("expiresAt", jwt.getExpiresAt());
        return me;
    }

    @PostMapping("/users")
    public ResponseEntity<UserResponse> createStoreUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = authService.createStoreUser(request);
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
    }
}
