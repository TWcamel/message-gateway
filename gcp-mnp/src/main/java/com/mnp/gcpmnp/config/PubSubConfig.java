package com.mnp.gcpmnp.config;

import com.mnp.commons.validation.Preconditions;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

import java.util.Optional;

/**
 * Pub/Sub topic/subscription names for this service's OUTBOX.
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "mnp.pubsub")
public class PubSubConfig {
    /** OUTBOX topic this service publishes to. */
    private String outboxTopic;

    /** Optional subscription (used only by adapter, but validated here). */
    private String outboxSubscription;

    /**
     * Spring Boot 3 + spring-cloud-gcp requires a Jackson message converter bean for Pub/SubTemplate.
     */
    @org.springframework.context.annotation.Bean
    public MessageConverter pubSubMessageConverter() {
        return new MappingJackson2MessageConverter();
    }
}
