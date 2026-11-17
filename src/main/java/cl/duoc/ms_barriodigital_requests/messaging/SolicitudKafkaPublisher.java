package cl.duoc.ms_barriodigital_requests.messaging;

import cl.duoc.ms_barriodigital_requests.dto.SolicitudKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SolicitudKafkaPublisher {

    private final KafkaTemplate<String, SolicitudKafkaEvent> kafkaTemplate;

    @Value("${app.kafka.topic.requests-events}")
    private String topic;

    public SolicitudKafkaPublisher(KafkaTemplate<String, SolicitudKafkaEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publicar(SolicitudKafkaEvent evento) {
        String key = (evento.getComunaId() != null && !evento.getComunaId().isBlank())
                ? evento.getComunaId()
                : "SIN_COMUNA";
        kafkaTemplate.send(topic, key, evento);
        log.info("[Kafka] Evento publicado: tipo={}, solicitudId={}, comuna={}",
                evento.getTipo(), evento.getSolicitudId(), key);
    }
}
