package com.joyeriaEcommerce.AureaTPO.ordenes.negocio;

import com.joyeriaEcommerce.AureaTPO.infraestructura.configuracion.RabbitConfiguration;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.EventoPedidoPendiente;
import com.joyeriaEcommerce.AureaTPO.ordenes.datos.EventoPedidoPendienteRepository;
import java.net.ConnectException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderProcessingMessagingTests {

    private final EventoPedidoPendienteRepository outbox = mock(EventoPedidoPendienteRepository.class);
    private final RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
    private final OrderProcessingPublisher publisher =
            new OrderProcessingPublisher(outbox, rabbitTemplate, mock(PlatformTransactionManager.class));

    private EventoPedidoPendiente pendiente() {
        var evento = new EventoPedidoPendiente("pedido-42-confirmado", "42");
        when(outbox.findTop20ByPublicadoIsNullAndProximoIntentoLessThanEqualOrderByCreadoAsc(any()))
                .thenReturn(List.of(evento));
        when(outbox.bloquear("pedido-42-confirmado")).thenReturn(Optional.of(evento));
        return evento;
    }

    @Test
    void publicaElPedidoDelOutboxYLoMarcaSoloConLaConfirmacionDelBroker() {
        var evento = pendiente();
        doAnswer(invocacion -> {
            CorrelationData correlacion = invocacion.getArgument(4);
            correlacion.getFuture().complete(new CorrelationData.Confirm(true, null));
            return null;
        }).when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        publisher.publicarPendientes();

        verify(rabbitTemplate).convertAndSend(eq(RabbitConfiguration.ORDERS_EXCHANGE),
                eq(RabbitConfiguration.ORDER_CONFIRMED_ROUTING_KEY), eq((Object) "42"),
                any(MessagePostProcessor.class), any(CorrelationData.class));
        assertThat(evento.publicado).isNotNull();
        assertThat(evento.intentos).isEqualTo(1);
    }

    @Test
    void conElBrokerCaidoElEventoQuedaPendienteYSeReintentaMasTarde() {
        var evento = pendiente();
        doThrow(new AmqpConnectException(new ConnectException("Broker caído")))
                .when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class),
                        any(MessagePostProcessor.class), any(CorrelationData.class));

        publisher.publicarPendientes();

        assertThat(evento.publicado).isNull();
        assertThat(evento.intentos).isEqualTo(1);
        assertThat(evento.proximoIntento).isAfter(Instant.now());
        assertThat(evento.ultimoError).contains("se reintentará");
    }

    @Test
    void unNackDelBrokerTampocoMarcaElEventoComoPublicado() {
        var evento = pendiente();
        doAnswer(invocacion -> {
            CorrelationData correlacion = invocacion.getArgument(4);
            correlacion.getFuture().complete(new CorrelationData.Confirm(false, "nack"));
            return null;
        }).when(rabbitTemplate).convertAndSend(any(String.class), any(String.class), any(Object.class),
                any(MessagePostProcessor.class), any(CorrelationData.class));

        publisher.publicarPendientes();

        assertThat(evento.publicado).isNull();
    }

    @Test
    void listenerDelegatesOrderCompletionToTheBusinessContract() {
        IPedidos pedidos = mock(IPedidos.class);
        OrderProcessingListener listener = new OrderProcessingListener(pedidos);

        listener.processConfirmedOrder("42");

        verify(pedidos).completeOrder(42L);
    }
}
