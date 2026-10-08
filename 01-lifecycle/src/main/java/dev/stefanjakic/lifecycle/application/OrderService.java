package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.Order;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Transactional
public class OrderService {

    private static final int MAX_IDEMPOTENCY_KEY_LENGTH = 100;

    private final OrderRepository repository;
    private final OrderCreationIdempotencyRepository idempotencyRepository;
    private final OrderAuditRepository auditRepository;

    public OrderService(
        OrderRepository repository,
        OrderCreationIdempotencyRepository idempotencyRepository,
        OrderAuditRepository auditRepository
    ) {
        this.repository = repository;
        this.idempotencyRepository = idempotencyRepository;
        this.auditRepository = auditRepository;
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
        auditRepository.append(
            order.id(),
            OrderAuditAction.CREATE,
            null,
            OrderStatus.CREATED
        );
        idempotencyRepository.complete(idempotencyKey, order.id());

        return order.id();
    }

    public void confirmOrder(String orderId) {
        Order order = load(orderId);
        OrderStatus previousStatus = order.status();

        order.confirm();
        repository.save(order);
        auditRepository.append(
            order.id(),
            OrderAuditAction.CONFIRM,
            previousStatus,
            order.status()
        );
    }

    public void shipOrder(String orderId) {
        Order order = load(orderId);
        OrderStatus previousStatus = order.status();

        order.ship();
        repository.save(order);
        auditRepository.append(
            order.id(),
            OrderAuditAction.SHIP,
            previousStatus,
            order.status()
        );
    }

    public void completeOrder(String orderId) {
        Order order = load(orderId);
        OrderStatus previousStatus = order.status();

        order.complete();
        repository.save(order);
        auditRepository.append(
            order.id(),
            OrderAuditAction.COMPLETE,
            previousStatus,
            order.status()
        );
    }

    public void cancelOrder(String orderId) {
        Order order = load(orderId);
        OrderStatus previousStatus = order.status();

        order.cancel();
        repository.save(order);
        auditRepository.append(
            order.id(),
            OrderAuditAction.CANCEL,
            previousStatus,
            order.status()
        );
    }

    @Transactional(readOnly = true)
    public OrderStatus statusOf(String orderId) {
        return load(orderId).status();
    }

    @Transactional(readOnly = true)
    public List<OrderAuditEntry> historyOf(String orderId) {
        load(orderId);
        return auditRepository.findByOrderId(orderId);
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
