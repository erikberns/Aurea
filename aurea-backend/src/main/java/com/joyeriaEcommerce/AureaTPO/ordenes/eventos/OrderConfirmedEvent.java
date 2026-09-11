package com.joyeriaEcommerce.AureaTPO.ordenes.eventos;

public record OrderConfirmedEvent(Long orderId, Long productId, Integer quantity, Long userId) {
}
