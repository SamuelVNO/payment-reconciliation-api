package io.github.samuelvno.paymentreconciliation.auth;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import io.github.samuelvno.paymentreconciliation.shared.persistence.BaseEntity;

// "AppUser" para não colidir com org.springframework.security.core.userdetails.User
@Entity
@Table(name = "users")
public class AppUser extends BaseEntity {

    @Column(nullable = false, length = 160, unique = true)
    private String email;

    // Só o hash (BCrypt). A senha em texto puro nunca é persistida nem logada.
    @Column(nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // Guardamos só o id: o token carrega o tenant e nenhuma regra aqui precisa da entidade Store.
    @Column(name = "store_id", updatable = false)
    private UUID storeId;

    protected AppUser() {
        // exigido pelo JPA
    }

    private AppUser(String email, String passwordHash, Role role, UUID storeId) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.storeId = storeId;
    }

    static AppUser admin(String email, String passwordHash) {
        return new AppUser(email, passwordHash, Role.ADMIN, null);
    }

    static AppUser storeUser(String email, String passwordHash, UUID storeId) {
        return new AppUser(email, passwordHash, Role.STORE_USER, storeId);
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public UUID getStoreId() {
        return storeId;
    }
}
