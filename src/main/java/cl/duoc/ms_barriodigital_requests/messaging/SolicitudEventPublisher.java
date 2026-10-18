package cl.duoc.ms_barriodigital_requests.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

/**
 * Publica los eventos de solicitudes en RabbitMQ.
 *
 * Cada evento se publica en DOS exchanges:
 * - solicitudes.direct -> llega a la cola especifica (solicitudes.creadas o solicitudes.estados)
 * - solicitudes.topic  -> llega a solicitudes.notificaciones (escucha solicitud.#)
 */
@Slf4j
@Service
public class SolicitudEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public SolicitudEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publicarSolicitudCreada(SolicitudCreadaEvent evento) {
        publicar(RabbitMQConfig.SOLICITUD_CREATED_ROUTING_KEY, evento);
    }

    public void publicarCambioEstado(SolicitudEstadoEvent evento) {
        publicar(RabbitMQConfig.SOLICITUD_STATUS_ROUTING_KEY, evento);
    }

    /**
     * Si RabbitMQ no esta disponible, se registra el error pero NO se propaga:
     * la solicitud ya quedo guardada en MySQL y el usuario no debe recibir un error por la notificacion.
     */
    private void publicar(String routingKey, Object evento) {
        try {
            rabbitTemplate.convertAndSend(RabbitMQConfig.SOLICITUDES_DIRECT_EXCHANGE, routingKey, evento);
            rabbitTemplate.convertAndSend(RabbitMQConfig.SOLICITUDES_TOPIC_EXCHANGE, routingKey, evento);
            log.info("Evento publicado [{}]: {}", routingKey, evento);
        } catch (AmqpException e) {
            log.error("No se pudo publicar el evento [{}] en RabbitMQ: {}", routingKey, e.getMessage());
        }
    }
}
