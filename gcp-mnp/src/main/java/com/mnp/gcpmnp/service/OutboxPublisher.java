package com.mnp.gcpmnp.service;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import com.mnp.commons.validation.Preconditions;
import com.mnp.gcpmnp.config.PubSubConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Publishes message payloads to the configured OUTBOX topic.
 * <p>Uses FP style (no var != null; Optional chains).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private final PubSubTemplate pubSubTemplate;
    private final PubSubConfig pubSubConfig;

    /**
     * Publishes the given payload (as object, serialized by Jackson) to the OUTBOX topic.
     *
     * @param payload serializable payload (e.g., Map with messageId)
     * @return completable future that completes with the GCP message id
     */
    public CompletableFuture<String> publish(final Map<String, ?> payload) {
        Preconditions.ensureNonNull(payload, "payload");
        final String topicId = Preconditions.ensureNonNull(pubSubConfig.getOutboxTopic(), "outboxTopic");

        log.info("Outbox publishing to topic={} payload={}", topicId, payload);

        // spring-cloud-gcp 6.x returns CompletableFuture
        return pubSubTemplate.publish(topicId, payload)
                .whenComplete((gcpId, ex) -> {
                    if (ex == null) {
                        log.info("Outbox published topic={} gcpMessageId={}", topicId, gcpId);
                    } else {
                        log.error("Outbox publish failed topic={}", topicId, ex);
                    }
                });
    }
}
