package com.mnp.natsadapter.service;

import com.mnp.commons.model.Direction;
import com.mnp.commons.util.NatsUtils;
import com.mnp.commons.validation.Preconditions;
import io.nats.client.Connection;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Manages connections to multiple NATS servers and publishes to each server's OUTBOX subject.
 * <p>Thread-safe. FP style: no {@code var != null}, relies on Streams + {@link Objects#nonNull}.</p>
 */
@Slf4j
@Service
public class MultiServerNatsService {

    private final List<NatsUtils.ServerSpec> specs;
    private final ConcurrentMap<String, Connection> connections = new ConcurrentHashMap<>();

    public MultiServerNatsService(final List<NatsUtils.ServerSpec> specs) {
        this.specs = Preconditions.ensureNotEmpty(specs, "natsServerSpecs");
        final Map<String, Connection> established = NatsUtils.connectAll(this.specs);
        connections.putAll(established);
    }

    /**
     * Publish the same UTF-8 payload to every connected server's OUTBOX subject.
     *
     * @param payloadUtf8 UTF-8 payload
     * @return number of servers that accepted the publish
     */
    public int publishToAllOutboxes(final String payloadUtf8) {
        final byte[] bytes = NatsUtils.toBytes(payloadUtf8);
        final Map<String, Connection> healthy = NatsUtils.healthyConnections(connections);

        final long accepted = healthy.entrySet().stream()
                .filter(e -> Objects.nonNull(e.getValue()))
                .map(e -> {
                    try {
                        final String subject = findSpec(e.getKey())
                                .map(s -> NatsUtils.subject(s, Direction.OUTBOX))
                                .orElse(null);
                        if (Objects.isNull(subject)) {
                            log.warn("Missing spec for NATS server [{}]", e.getKey());
                            return false;
                        }
                        e.getValue().publish(subject, bytes);
                        log.debug("Published to NATS [{}] subject={}", e.getKey(), subject);
                        return true;
                    } catch (final Exception ex) {
                        log.error("Failed to publish to NATS [{}]", e.getKey(), ex);
                        return false;
                    }
                })
                .filter(Boolean::booleanValue)
                .count();
        return (int) accepted;
    }

    /** Snapshot of healthy connections (for actuators / debugging). */
    public List<String> healthyServerNames() {
        return Collections.unmodifiableList(
                NatsUtils.healthyConnections(connections).keySet().stream()
                        .sorted()
                        .collect(Collectors.toList())
        );
    }

    @PreDestroy
    public void shutdown() {
        NatsUtils.closeAll(connections);
    }

    private java.util.Optional<NatsUtils.ServerSpec> findSpec(final String serverName) {
        return specs.stream()
                .filter(s -> s.name().equals(serverName))
                .findFirst();
    }
}
