package cl.duoc.ms_barriodigital_requests.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    // 7 días en milisegundos
    private static final String RETENCION_7_DIAS = "604800000";

    // Topic de eventos del trámite según la topología del caso. Spring lo crea al arrancar si no existe
    @Bean
    public NewTopic requestsEventsTopic(@Value("${requests.kafka.topic}") String topic,
                                        @Value("${KAFKA_REPLICAS:1}") int replicas) {
        return TopicBuilder.name(topic)
                .partitions(3)
                .replicas(replicas)
                .config(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_DELETE)
                .config(TopicConfig.RETENTION_MS_CONFIG, RETENCION_7_DIAS)
                .build();
    }
}
