package io.github.samuelvno.paymentreconciliation.auth;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

import javax.crypto.spec.SecretKeySpec;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import io.github.samuelvno.paymentreconciliation.TestcontainersConfiguration;
import io.github.samuelvno.paymentreconciliation.shared.security.SecurityProperties;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fluxo real, sem atalhos: login -> JWT assinado -> filtros do Spring Security -> controller. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SecurityIT {

    private static final AtomicLong DOCUMENTS = new AtomicLong(40_000_000_000_000L);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityProperties properties;

    @Test
    void storeUserOnlySeesItsOwnStore() throws Exception {
        String admin = login(properties.admin().email(), properties.admin().password());
        String storeA = createStore(admin, "Loja A");
        String storeB = createStore(admin, "Loja B");
        String email = "financeiro+" + System.nanoTime() + "@loja-a.com";

        mockMvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "senha-forte-123", "storeId": "%s"}
                                """.formatted(email, storeA)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STORE_USER"));
        String user = login(email, "senha-forte-123");

        mockMvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(jsonPath("$.storeId").value(storeA));
        mockMvc.perform(get("/api/stores/{id}/orders", storeA).header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/stores/{id}/orders", storeB).header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"));
        mockMvc.perform(get("/api/stores").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .andExpect(status().isForbidden());
    }

    @Test
    void wrongPasswordAndUnknownEmailGetTheSameAnswer() throws Exception {
        mockMvc.perform(loginRequest(properties.admin().email(), "senha-errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
        mockMvc.perform(loginRequest("ninguem@nada.com", "senha-errada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void rejectsTamperedAndExpiredTokens() throws Exception {
        String admin = login(properties.admin().email(), properties.admin().password());
        String tampered = admin.substring(0, admin.length() - 4) + "AAAA";

        mockMvc.perform(get("/api/stores").header(HttpHeaders.AUTHORIZATION, bearer(tampered)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/stores").header(HttpHeaders.AUTHORIZATION, bearer(expiredAdminToken())))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/stores"))
                .andExpect(status().isUnauthorized());
    }

    private String expiredAdminToken() {
        var key = new SecretKeySpec(properties.jwt().secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        Instant past = Instant.now().minusSeconds(7200);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(past)
                .expiresAt(past.plusSeconds(60))
                .subject("admin")
                .claim("role", "ADMIN")
                .build();
        return new NimbusJwtEncoder(new ImmutableSecret<>(key))
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private String createStore(String adminToken, String name) throws Exception {
        String body = mockMvc.perform(post("/api/stores").header(HttpHeaders.AUTHORIZATION, bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s", "document": "%d", "feeRate": 0.0299}
                                """.formatted(name, DOCUMENTS.incrementAndGet())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    private String login(String email, String password) throws Exception {
        String body = mockMvc.perform(loginRequest(email, password))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    private static RequestBuilder loginRequest(String email, String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password));
    }

    private static String bearer(String token) {
        return "Bearer " + token;
    }
}
