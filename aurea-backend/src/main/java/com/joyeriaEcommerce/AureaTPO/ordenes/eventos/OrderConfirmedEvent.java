package com.joyeriaEcommerce.AureaTPO.ordenes.eventos;

import java.util.Map;

public record OrderConfirmedEvent(Long orderId, Map<Long, Integer> productQuantities, Long userId) {
}
