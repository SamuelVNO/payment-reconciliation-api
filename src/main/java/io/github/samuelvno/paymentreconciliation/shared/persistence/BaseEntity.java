package io.github.samuelvno.paymentreconciliation.shared.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import org.hibernate.Hibernate;

/**
 * Base para as entidades: id UUID, timestamps de auditoria e equals/hashCode seguros para JPA.
 */
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // Callbacks JPA rodam no persist(), antes do flush. @CreationTimestamp só preenche
    // no INSERT (flush/commit), então o DTO montado dentro da transação saía com null.
    @PrePersist
    void onPrePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onPreUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    // Igualdade pelo id, mas só depois de persistida: antes do persist o id é null.
    // Hibernate.getClass() "desembrulha" proxies de lazy loading.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        BaseEntity other = (BaseEntity) o;
        return getId() != null && getId().equals(other.getId());
    }

    // hashCode constante por classe: não muda quando o id é gerado,
    // então a entidade não "some" de um HashSet depois do persist.
    @Override
    public int hashCode() {
        return Hibernate.getClass(this).hashCode();
    }
}
