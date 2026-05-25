package com.mnp.natsadapter.bridge;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.mnp.commons.util.NatsUtils;
import com.mnp.commons.validation.Preconditions;
import com.mnp.natsadapter.config.PubSubInboxConfig;
import com.mnp.natsadapter.service.MultiServerNatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Subscribes to the GCP Pub/Sub INBOX topic and bridges every message
 * to every NATS server's OUTBOX subject.
 * <p>Uses the low-level PubSubSubscriberTemplate to receive raw bytes,
 * which avoids Jackson deserialization assumptions on the GCP side.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PubSubToNatsBridge {

    private final PubSubTemplate pubSubTemplate;
    private final PubSubInboxConfig inboxConfig;
    private final MultiServerNatsService natsService;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void startBridge() {
        final String subscription = Preconditions.ensureNonNull(
                inboxConfig.getInboxSubscription(), "inboxSubscription");
        log.info("Starting Pub/Sub → NATS bridge on subscription={}", subscription);

        // Subscribe using the async subscriber; forward each message to all NATS outboxes
        pubSubTemplate.subscribe(subscription, message -> {
            try {
                final byte[] data = message.getPubsubMessage().getData().toByteArray();
                final String payloadUtf8 = NatsUtils.toUtf8(data);
                final int accepted = natsService.publishToAllOutboxes(payloadUtf8);
                log.info("Bridged 1 message from Pub/Sub to {} NATS outbox(es)", accepted);
                message.ack();
            } catch (final Exception ex) {
                log.error("Bridge failed, nacking message", ex);
                message.nack();
            }
        });
    }
}
