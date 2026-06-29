package com.oms.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.order-placed}")
    private String orderPlacedTopic;

    @Value("${app.kafka.topics.order-cancelled}")
    private String orderCancelledTopic;

    @Bean
    public NewTopic orderPlacedTopic() {
        return TopicBuilder
                .name(orderPlacedTopic)
                .partitions(3)      // 3 partitions = up to 3 parallel consumers
                .replicas(1)        // 1 replica for local dev (use 3 in production)
                .build();
    }

    @Bean
    public NewTopic orderCancelledTopic() {
        return TopicBuilder
                .name(orderCancelledTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
