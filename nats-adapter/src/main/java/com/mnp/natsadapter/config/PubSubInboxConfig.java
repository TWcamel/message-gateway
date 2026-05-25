package com.mnp.natsadapter.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;

/**
 * INBOX subscription that this adapter listens to on GCP Pub/Sub.
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "mnp.pubsub")
public class PubSubInboxConfig {
    /** Topic to subscribe from (gcp-mnp's OUTBOX). */
    private String inboxTopic;

    /** Corresponding subscription id. */
    private String inboxSubscription;

    /** Jackson converter required by PubSubInboundChannelAdapter / template. */
    @org.springframework.context.annotation.Bean
    public MessageConverter pubSubMessageConverter() {
        return new MappingJackson2MessageConverter();
    }
}
