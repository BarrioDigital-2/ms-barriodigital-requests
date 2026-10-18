package cl.duoc.ms_barriodigital_requests.messaging;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de RabbitMQ del lado productor (ms-requests).
 * Los nombres DEBEN coincidir con los de ms-notifications (consumidor).
 */
@Configuration
public class RabbitMQConfig {

    // ========== EXCHANGES ==========
    public static final String SOLICITUDES_DIRECT_EXCHANGE = "solicitudes.direct";
    public static final String SOLICITUDES_TOPIC_EXCHANGE = "solicitudes.topic";

    // ========== ROUTING KEYS ==========
    public static final String SOLICITUD_CREATED_ROUTING_KEY = "solicitud.created";
    public static final String SOLICITUD_STATUS_ROUTING_KEY = "solicitud.status";

    /** RabbitTemplate convierte los eventos a JSON con este conversor (Spring Boot lo aplica solo). */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    /**
     * Se declaran tambien aqui (mismos parametros que en ms-notifications) para que existan
     * aunque ms-notifications no haya arrancado nunca. Declarar algo que ya existe igual no hace nada.
     */
    @Bean
    public DirectExchange solicitudesDirectExchange() {
        return new DirectExchange(SOLICITUDES_DIRECT_EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange solicitudesTopicExchange() {
        return new TopicExchange(SOLICITUDES_TOPIC_EXCHANGE, true, false);
    }
}
