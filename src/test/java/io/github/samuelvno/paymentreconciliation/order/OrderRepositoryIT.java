package io.github.samuelvno.paymentreconciliation.order;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import io.github.samuelvno.paymentreconciliation.TestcontainersConfiguration;
import io.github.samuelvno.paymentreconciliation.store.Store;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// @DataJpaTest trocaria o Postgres por um H2 em memória; replace = NONE mantém o banco real do container.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class OrderRepositoryIT {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void uniqueConstraintBlocksDuplicatedExternalIdInSameStore() {
        Store store = em.persist(new Store("Loja A", "11111111000111", new BigDecimal("0.0299")));
        orderRepository.saveAndFlush(new Order(store, "PED-1", new BigDecimal("10.00"), LocalDate.now()));

        assertThatThrownBy(() ->
                orderRepository.saveAndFlush(new Order(store, "PED-1", new BigDecimal("20.00"), LocalDate.now())))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByIdAndStoreIdDoesNotLeakOrdersFromOtherStores() {
        Store storeA = em.persist(new Store("Loja A", "11111111000111", new BigDecimal("0.0299")));
        Store storeB = em.persist(new Store("Loja B", "22222222000122", new BigDecimal("0.0299")));
        Order order = em.persist(new Order(storeA, "PED-1", new BigDecimal("10.00"), LocalDate.now()));
        em.flush();

        assertThat(orderRepository.findByIdAndStoreId(order.getId(), storeA.getId())).isPresent();
        assertThat(orderRepository.findByIdAndStoreId(order.getId(), storeB.getId())).isEmpty();
    }
}
