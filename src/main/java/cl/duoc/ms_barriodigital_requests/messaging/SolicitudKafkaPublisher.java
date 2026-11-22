package cl.duoc.ms_barriodigital_requests.messaging;

import cl.duoc.ms_barriodigital_requests.dto.SolicitudKafkaEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SolicitudKafkaPublisher {

    private final KafkaTemplate<String, SolicitudKafkaEvent> kafkaTemplate;
    private final String topic;

    public SolicitudKafkaPublisher(KafkaTemplate<String, SolicitudKafkaEvent> kafkaTemplate,
                                   @Value("${requests.kafka.topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publicar(SolicitudKafkaEvent evento) {
        // Key = id de la solicitud: todos los eventos de un mismo trámite van a la misma partición
        // y se leen en orden (CREADA → ADMITIDO → RESUELTO)
        String key = String.valueOf(evento.getId());
        kafkaTemplate.send(topic, key, evento);
        log.info("📤 [Kafka] Evento publicado en {}: tipo={}, id={}", topic, evento.getTipo(), evento.getId());
    }
}
