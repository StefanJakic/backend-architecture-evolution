package dev.stefanjakic.lifecycle.api;

import dev.stefanjakic.lifecycle.application.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<OrderResponse> createOrder(
        @RequestHeader(value = "Idempotency-Key", required = false)
        String idempotencyKey
    ) {
        String orderId = service.createOrder(idempotencyKey);

        return ResponseEntity
            .created(URI.create("/orders/" + orderId))
            .body(response(orderId));
    }

    @GetMapping("/{orderId}")
    OrderResponse getOrder(@PathVariable String orderId) {
        return response(orderId);
    }

    @GetMapping("/{orderId}/history")
    List<OrderAuditResponse> getOrderHistory(@PathVariable String orderId) {
        return service.historyOf(orderId)
            .stream()
            .map(OrderAuditResponse::of)
            .toList();
    }

    @PostMapping("/{orderId}/confirm")
    OrderResponse confirmOrder(@PathVariable String orderId) {
        service.confirmOrder(orderId);
        return response(orderId);
    }

    @PostMapping("/{orderId}/ship")
    OrderResponse shipOrder(@PathVariable String orderId) {
        service.shipOrder(orderId);
        return response(orderId);
    }

    @PostMapping("/{orderId}/complete")
    OrderResponse completeOrder(@PathVariable String orderId) {
        service.completeOrder(orderId);
        return response(orderId);
    }

    @PostMapping("/{orderId}/cancel")
    OrderResponse cancelOrder(@PathVariable String orderId) {
        service.cancelOrder(orderId);
        return response(orderId);
    }

    private OrderResponse response(String orderId) {
        return OrderResponse.of(orderId, service.statusOf(orderId));
    }
}
