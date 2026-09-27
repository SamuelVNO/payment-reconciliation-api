package io.github.samuelvno.paymentreconciliation.order;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.samuelvno.paymentreconciliation.shared.error.BusinessConflictException;
import io.github.samuelvno.paymentreconciliation.shared.error.ResourceNotFoundException;
import io.github.samuelvno.paymentreconciliation.store.Store;
import io.github.samuelvno.paymentreconciliation.store.StoreService;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final StoreService storeService;

    public OrderService(OrderRepository orderRepository, StoreService storeService) {
        this.orderRepository = orderRepository;
        this.storeService = storeService;
    }

    @Transactional
    public OrderResponse create(UUID storeId, CreateOrderRequest request) {
        Store store = storeService.getStore(storeId);
        if (orderRepository.existsByStoreIdAndExternalId(storeId, request.externalId())) {
            throw new BusinessConflictException("Order %s already exists for this store".formatted(request.externalId()));
        }
        Order order = new Order(store, request.externalId(), request.amount(), request.orderDate());
        return OrderResponse.from(orderRepository.save(order));
    }

    public OrderResponse findById(UUID storeId, UUID orderId) {
        return orderRepository.findByIdAndStoreId(orderId, storeId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
    }

    public Page<OrderResponse> findAll(UUID storeId, ReconciliationStatus status, Pageable pageable) {
        Page<Order> orders = status == null
                ? orderRepository.findByStoreId(storeId, pageable)
                : orderRepository.findByStoreIdAndReconciliationStatus(storeId, status, pageable);
        return orders.map(OrderResponse::from);
    }
}
