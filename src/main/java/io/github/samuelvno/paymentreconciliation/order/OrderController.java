package io.github.samuelvno.paymentreconciliation.order;

import java.net.URI;
import java.util.UUID;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Pedidos", description = "Pedidos que a loja espera receber.")
@RequestMapping("/api/stores/{storeId}/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Cadastra um pedido")
    @PostMapping
    public ResponseEntity<OrderResponse> create(@PathVariable UUID storeId,
                                                @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.create(storeId, request);
        return ResponseEntity.created(URI.create("/api/stores/%s/orders/%s".formatted(storeId, order.id())))
                .body(order);
    }

    @Operation(summary = "Busca um pedido")
    @GetMapping("/{orderId}")
    public OrderResponse findById(@PathVariable UUID storeId, @PathVariable UUID orderId) {
        return orderService.findById(storeId, orderId);
    }

    @Operation(summary = "Lista pedidos, com filtro opcional por status de conciliação")
    @GetMapping
    public Page<OrderResponse> findAll(@PathVariable UUID storeId,
                                       @RequestParam(required = false) ReconciliationStatus status,
                                       @PageableDefault(size = 20, sort = "orderDate", direction = Sort.Direction.DESC)
                                       Pageable pageable) {
        return orderService.findAll(storeId, status, pageable);
    }
}
