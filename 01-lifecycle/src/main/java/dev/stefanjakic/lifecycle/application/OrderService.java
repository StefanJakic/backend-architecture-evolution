package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.Order;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Transactional
public class OrderService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final OrderRepository repository;
    private final OrderCreationIdempotencyRepository idempotencyRepository;

    public OrderService(
        OrderRepository repository,
        OrderCreationIdempotencyRepository idempotencyRepository
    ) {
        this.repository = repository;
        this.idempotencyRepository = idempotencyRepository;
    }

    public String createOrder(String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);

        var completedOrderId =
            idempotencyRepository.findCompletedOrderId(idempotencyKey);

        if (completedOrderId.isPresent()) {
            return completedOrderId.get();
        }

        boolean claimed = idempotencyRepository.tryClaim(idempotencyKey);

        if (!claimed) {
            return idempotencyRepository.findCompletedOrderId(idempotencyKey)
                .orElseThrow(
                    () -> new IdempotencyResultUnavailableException(idempotencyKey)
                );
        }

        Order order = new Order(UUID.randomUUID().toString());
        repository.save(order);
        idempotencyRepository.complete(idempotencyKey, order.id());

        return order.id();
    }

    public void confirmOrder(String orderId) {
        Order order = load(orderId);
        order.confirm();
        repository.save(order);
    }

    public void shipOrder(String orderId) {
        Order order = load(orderId);
        order.ship();
        repository.save(order);
    }

    public void completeOrder(String orderId) {
        Order order = load(orderId);
        order.complete();
        repository.save(order);
    }

    public void cancelOrder(String orderId) {
        Order order = load(orderId);
        order.cancel();
        repository.save(order);
    }

    @Transactional(readOnly = true)
    public OrderStatus statusOf(String orderId) {
        return load(orderId).status();
    }

    private Order load(String orderId) {
        return repository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private void validateIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new InvalidIdempotencyKeyException(
                "Idempotency-Key must not be blank"
            );
        }

        if (idempotencyKey.length() > MAX_IDEMPOTENCY_KEY_LENGTH) {
            throw new InvalidIdempotencyKeyException(
                "Idempotency-Key must not exceed 100 characters"
            );
        }
    }
}
