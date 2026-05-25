package com.mnp.natsconsumer.consumer;

import com.mnp.commons.model.Direction;
import com.mnp.commons.util.NatsUtils;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import io.nats.client.Connection;
import io.nats.client.Dispatcher;
import io.nats.client.Message;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Subscribes to the INBOX subject of every configured NATS server.
 * Logs each received message using FP style with Streams + {@link Objects#nonNull}.
 */
@Slf4j
@Component
public class InboxSubscriber {

    private final ConcurrentMap<String, Connection> connections = new ConcurrentHashMap<>();

    public InboxSubscriber(final List<NatsUtils.ServerSpec> specs) {
        // Establish all server connections
        final Map<String, Connection> established = NatsUtils.connectAll(specs);
        connections.putAll(established);

        // For each connected server, subscribe to its INBOX
        specs.stream()
                .filter(Objects::nonNull)
                .filter(spec -> connections.containsKey(spec.name()))
                .forEach(spec -> subscribe(spec, connections.get(spec.name())));
    }

    private void subscribe(final NatsUtils.ServerSpec spec, final Connection conn) {
        final String subject = spec.subject(Direction.INBOX);
        final Dispatcher dispatcher = conn.createDispatcher(msg -> handleMessage(spec.name(), subject, msg));
        dispatcher.subscribe(subject);
        log.info("Subscribed to NATS [{}] inbox subject={}", spec.name(), subject);
    }

    private void handleMessage(final String serverName, final String subject, final Message msg) {
        final String payload = NatsUtils.toUtf8(msg.getData());
        log.info("Received from NATS [{}] subject={} payload={}", serverName, subject, payload);
    }

    /** Snapshot of server names we are currently consuming. */
    public List<String> consumingServerNames() {
        return NatsUtils.healthyConnections(connections).keySet().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    @PreDestroy
    public void shutdown() {
        NatsUtils.closeAll(connections);
    }
}
