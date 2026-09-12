package com.joyeriaEcommerce.AureaTPO.notificaciones.negocio;

import com.joyeriaEcommerce.AureaTPO.ordenes.eventos.OrderConfirmedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

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

    @PostConstruct
    public void init() {
        System.out.println("[LIFECYCLE] NotificationService inicializado y listo para escuchar eventos de confirmación.");
    }

    @PreDestroy
    public void cleanup() {
        System.out.println("[LIFECYCLE] Apagando NotificationService. Cerrando canales de comunicación asíncronos...");
    }
}
