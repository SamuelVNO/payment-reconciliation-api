package io.github.samuelvno.paymentreconciliation.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresIn) {
}
