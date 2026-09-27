package io.github.samuelvno.paymentreconciliation.auth;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Autenticação", description = "Login, usuário atual e cadastro de usuários de loja.")
@RequestMapping("/api")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Troca e-mail e senha por um JWT")
    @PostMapping("/auth/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Operation(summary = "Dados do usuário do token")
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

    @Operation(summary = "Cria usuário de uma loja (somente ADMIN)")
    @PostMapping("/users")
    public ResponseEntity<UserResponse> createStoreUser(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = authService.createStoreUser(request);
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
    }
}
