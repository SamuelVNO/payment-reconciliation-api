package io.github.samuelvno.paymentreconciliation.auth;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Email @Size(max = 160) String email,
        // limite de 72: o BCrypt ignora silenciosamente o que passa de 72 bytes
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotNull UUID storeId) {
}
