package cl.duoc.ms_barriodigital_requests.messaging;

import cl.duoc.ms_barriodigital_requests.dto.SolicitudKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SolicitudKafkaPublisher {

    private static final String TOPIC = "solicitudes-eventos";
    private final KafkaTemplate<String, SolicitudKafkaEvent> kafkaTemplate;

    public void publicar(SolicitudKafkaEvent evento) {
        // Key = comunaId para que eventos de la misma comuna vayan a la misma partición
        kafkaTemplate.send(TOPIC, evento.getComunaId(), evento);
        log.info("📤 [Kafka] Evento publicado: tipo={}, id={}, comuna={}",
                evento.getTipo(), evento.getId(), evento.getComunaId());
    }
}