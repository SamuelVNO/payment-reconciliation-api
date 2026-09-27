package io.github.samuelvno.paymentreconciliation.auth;

import java.util.UUID;

public record UserResponse(UUID id, String email, Role role, UUID storeId) {

    static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getStoreId());
    }
}
