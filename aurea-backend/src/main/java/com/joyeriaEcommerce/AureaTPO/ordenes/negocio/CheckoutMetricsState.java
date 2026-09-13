package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class CheckoutMetricsState {

    private final AtomicLong confirmedOrdersCount = new AtomicLong(0);
    private volatile Long lastConfirmedOrderId;
    private volatile LocalDateTime lastConfirmationAt;

    @PostConstruct
    public void init() {
        System.out.println("[LIFECYCLE] CheckoutMetricsState inicializado. Estado de metricas de checkout listo.");
    }

    @org.springframework.transaction.event.TransactionalEventListener(phase=org.springframework.transaction.event.TransactionPhase.AFTER_COMMIT)
    public void alConfirmar(com.joyeriaEcommerce.AureaTPO.ordenes.eventos.OrderConfirmedEvent event){recordConfirmedOrder(event.orderId());}
    public void recordConfirmedOrder(Long orderId) {
        confirmedOrdersCount.incrementAndGet();
        lastConfirmedOrderId = orderId;
        lastConfirmationAt = LocalDateTime.now();
    }

    public long getConfirmedOrdersCount() {
        return confirmedOrdersCount.get();
    }

    public Long getLastConfirmedOrderId() {
        return lastConfirmedOrderId;
    }

    public LocalDateTime getLastConfirmationAt() {
        return lastConfirmationAt;
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("[LIFECYCLE] CheckoutMetricsState destruido. Ordenes confirmadas durante la ejecucion: "
                + confirmedOrdersCount.get());
    }
}
