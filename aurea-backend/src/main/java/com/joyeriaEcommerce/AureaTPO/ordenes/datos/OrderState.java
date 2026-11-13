package com.joyeriaEcommerce.AureaTPO.ordenes.datos;

/**
 * Patrón State (GoF) para el ciclo de vida de la entidad central Order.
 * Cada estado concreto decide qué transiciones permite; cualquier combinación
 * no permitida arroja TransicionEstadoInvalidaException (HTTP 409 Conflict).
 */
public sealed interface OrderState permits
        OrderState.PendingState,
        OrderState.ConfirmedState,
        OrderState.CompletedState,
        OrderState.CancelledState {

    OrderStatus toStatus();

    default OrderState confirmar() {
        throw new TransicionEstadoInvalidaException(toStatus(), "confirmar");
    }

    default OrderState completar() {
        throw new TransicionEstadoInvalidaException(toStatus(), "completar");
    }

    default OrderState cancelar() {
        throw new TransicionEstadoInvalidaException(toStatus(), "cancelar");
    }

    static OrderState fromStatus(OrderStatus status) {
        if (status == null) {
            return new PendingState();
        }
        return switch (status) {
            case PENDING -> new PendingState();
            case CONFIRMED -> new ConfirmedState();
            case COMPLETED -> new CompletedState();
            case CANCELLED -> new CancelledState();
        };
    }

    final class PendingState implements OrderState {
        @Override
        public OrderStatus toStatus() {
            return OrderStatus.PENDING;
        }

        @Override
        public OrderState confirmar() {
            return new ConfirmedState();
        }

        @Override
        public OrderState cancelar() {
            return new CancelledState();
        }
    }

    final class ConfirmedState implements OrderState {
        @Override
        public OrderStatus toStatus() {
            return OrderStatus.CONFIRMED;
        }

        @Override
        public OrderState completar() {
            return new CompletedState();
        }

        @Override
        public OrderState cancelar() {
            return new CancelledState();
        }
    }

    final class CompletedState implements OrderState {
        @Override
        public OrderStatus toStatus() {
            return OrderStatus.COMPLETED;
        }

        @Override
        public OrderState cancelar() {
            return new CancelledState();
        }
    }

    final class CancelledState implements OrderState {
        @Override
        public OrderStatus toStatus() {
            return OrderStatus.CANCELLED;
        }
    }
}

