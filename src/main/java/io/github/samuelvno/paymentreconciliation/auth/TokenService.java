package io.github.samuelvno.paymentreconciliation.auth;

import java.time.Clock;
import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import io.github.samuelvno.paymentreconciliation.shared.security.SecurityProperties;
import io.github.samuelvno.paymentreconciliation.shared.security.TenantClaims;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final SecurityProperties properties;
    private final Clock clock;

    public TokenService(JwtEncoder jwtEncoder, SecurityProperties properties, Clock clock) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    public TokenResponse issue(AppUser user) {
        Instant now = Instant.now(clock);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(properties.jwt().issuer())
                .issuedAt(now)
                .expiresAt(now.plus(properties.jwt().ttl()))
                .subject(user.getId().toString())
                .claim(TenantClaims.EMAIL, user.getEmail())
                .claim(TenantClaims.ROLE, user.getRole().name());
        if (user.getStoreId() != null) {
            claims.claim(TenantClaims.STORE_ID, user.getStoreId().toString());
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
        return new TokenResponse(token, "Bearer", properties.jwt().ttl().toSeconds());
    }
}
