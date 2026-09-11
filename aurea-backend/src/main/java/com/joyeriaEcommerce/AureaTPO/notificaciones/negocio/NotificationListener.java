package com.joyeriaEcommerce.AureaTPO.notificaciones.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.eventos.OrderConfirmedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NotificationListener {

    @EventListener
    @Async
    public void onOrderConfirmed(OrderConfirmedEvent event) {
        // En una implementación real, aquí enviaríamos un email con JavaMailSender o AWS SES
        System.out.println("=============================================");
        System.out.println("🔔 [NOTIFICACIÓN] Enviando correo al usuario ID: " + event.userId());
        System.out.println("🔔 [NOTIFICACIÓN] Su orden #" + event.orderId() + " ha sido confirmada con éxito.");
        System.out.println("🔔 [NOTIFICACIÓN] Detalles: " + event.quantity() + " unidades del producto ID " + event.productId());
        System.out.println("=============================================");
    }
}
