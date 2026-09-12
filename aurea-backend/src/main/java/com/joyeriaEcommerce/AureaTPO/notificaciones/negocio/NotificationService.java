package com.joyeriaEcommerce.AureaTPO.notificaciones.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.eventos.OrderConfirmedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    @EventListener
    @Async
    public void handleOrderConfirmedEvent(OrderConfirmedEvent event) {
        // En una aplicación real, aquí se enviaría un correo electrónico
        System.out.println("=========================================================");
        System.out.println("[OBSERVER PATTERN] - NotificationService escuchó el evento!");
        System.out.println("Enviando correo electrónico de confirmación para la orden: " + event.orderId());
        System.out.println("Usuario ID destinatario: " + event.userId());
        System.out.println("=========================================================");
    }
}
