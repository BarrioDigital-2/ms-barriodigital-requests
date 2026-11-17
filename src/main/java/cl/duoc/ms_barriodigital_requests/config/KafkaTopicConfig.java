package cl.duoc.ms_barriodigital_requests.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topic.requests-events}")
    private String topic;

    @Value("${app.kafka.topic.replicas}")
    private int replicas;

    @Bean
    public NewTopic requestsEventsTopic() {
        return TopicBuilder
                .name(topic)
                .partitions(3)
                .replicas(replicas)
                .config("cleanup.policy", "delete")
                .config("retention.ms", String.valueOf(7L * 24 * 60 * 60 * 1000))
                .build();
    }
}
