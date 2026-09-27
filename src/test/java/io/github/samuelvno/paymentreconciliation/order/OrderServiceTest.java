package io.github.samuelvno.paymentreconciliation.order;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.samuelvno.paymentreconciliation.shared.error.BusinessConflictException;
import io.github.samuelvno.paymentreconciliation.shared.error.ResourceNotFoundException;
import io.github.samuelvno.paymentreconciliation.store.Store;
import io.github.samuelvno.paymentreconciliation.store.StoreService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID STORE_ID = UUID.randomUUID();

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StoreService storeService;

    @InjectMocks
    private OrderService orderService;

    private final CreateOrderRequest request =
            new CreateOrderRequest("PED-1", new BigDecimal("150.00"), LocalDate.of(2026, 9, 20));

    @Test
    void createsOrderWhenExternalIdIsNew() {
        when(storeService.getStore(STORE_ID)).thenReturn(new Store("Loja", "12345678000190", new BigDecimal("0.0299")));
        when(orderRepository.existsByStoreIdAndExternalId(STORE_ID, "PED-1")).thenReturn(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = orderService.create(STORE_ID, request);

        ArgumentCaptor<Order> saved = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(saved.capture());
        assertThat(saved.getValue().getReconciliationStatus()).isEqualTo(ReconciliationStatus.PENDING);
        assertThat(response.externalId()).isEqualTo("PED-1");
        assertThat(response.amount()).isEqualByComparingTo("150.00");
    }

    @Test
    void rejectsDuplicatedExternalId() {
        when(storeService.getStore(STORE_ID)).thenReturn(new Store("Loja", "12345678000190", new BigDecimal("0.0299")));
        when(orderRepository.existsByStoreIdAndExternalId(STORE_ID, "PED-1")).thenReturn(true);

        assertThatThrownBy(() -> orderService.create(STORE_ID, request))
                .isInstanceOf(BusinessConflictException.class)
                .hasMessageContaining("PED-1");
        verify(orderRepository, never()).save(any());
    }

    @Test
    void throwsNotFoundWhenOrderBelongsToAnotherStore() {
        UUID orderId = UUID.randomUUID();
        when(orderRepository.findByIdAndStoreId(orderId, STORE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.findById(STORE_ID, orderId))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
