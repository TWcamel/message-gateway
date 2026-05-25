package com.mnp.commons.util;

import com.mnp.commons.model.Direction;
import com.mnp.commons.validation.Preconditions;
import io.nats.client.Connection;
import io.nats.client.ConnectionListener;
import io.nats.client.Nats;
import io.nats.client.Options;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Reusable NATS utilities: multi-server connection, subject naming, message conversion.
 * <p>Uses Stream API and {@link Objects#nonNull} instead of {@code var != null}.</p>
 */
@Slf4j
public final class NatsUtils {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(5);

    /**
     * Spec for one NATS server cluster — each spec maps to an isolated inbox/outbox subject namespace.
     */
    public record ServerSpec(
            String name,
            List<String> urls,
            String inboxSubject,
            String outboxSubject
    ) {
        public ServerSpec {
            Preconditions.ensureNotBlank(name, "serverSpec.name");
            Preconditions.ensureNotEmpty(urls, "serverSpec.urls");
            Preconditions.ensureNotBlank(inboxSubject, "serverSpec.inboxSubject");
            Preconditions.ensureNotBlank(outboxSubject, "serverSpec.outboxSubject");
        }

        public String subject(final Direction direction) {
            return switch (Preconditions.ensureNonNull(direction, "direction")) {
                case INBOX -> inboxSubject;
                case OUTBOX -> outboxSubject;
            };
        }
    }

    private NatsUtils() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Default (noop) connection listener backed by SLF4J.
     */
    public static ConnectionListener loggingListener(final String serverName) {
        final ConnectionListener delegate = (conn, type) ->
                log.info("NATS [{}] connection lifecycle: {}", serverName, type);
        return delegate;
    }

    /**
     * Establish connections to every server spec. Returns an UNMODIFIABLE map keyed by server name.
     * Any failed connection is logged and skipped (FP-friendly: no exception thrown for partial success).
     */
    public static Map<String, Connection> connectAll(final List<ServerSpec> specs) {
        Preconditions.ensureNotEmpty(specs, "serverSpecs");

        // Quiet jnats internal JUL logger in validation environment
        Logger.getLogger("io.nats").setLevel(Level.OFF);

        final Map<String, Connection> connections = specs.stream()
                .filter(Objects::nonNull)
                .map(spec -> establishOne(spec).map(c -> Map.entry(spec.name(), c)).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));

        log.info("Connected to {}/{} NATS server(s): {}", connections.size(), specs.size(), connections.keySet());
        return connections;
    }

    /**
     * Build subject for a given server using the spec's configured inbox/outbox.
     */
    public static String subject(final ServerSpec spec, final Direction direction) {
        return Preconditions.ensureNonNull(spec, "serverSpec").subject(direction);
    }

    /**
     * Convert UTF-8 payload string to byte[].
     */
    public static byte[] toBytes(final String payload) {
        return Preconditions.ensureNonNull(payload, "payload").getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Convert byte[] back to UTF-8 string.
     */
    public static String toUtf8(final byte[] bytes) {
        return new String(Preconditions.ensureNonNull(bytes, "bytes"), StandardCharsets.UTF_8);
    }

    /**
     * Filter to only healthy (CONNECTED) connections.
     */
    public static Map<String, Connection> healthyConnections(final Map<String, Connection> connections) {
        if (Objects.isNull(connections)) {
            return Collections.emptyMap();
        }
        return connections.entrySet().stream()
                .filter(e -> Objects.nonNull(e.getValue()))
                .filter(e -> io.nats.client.Connection.Status.CONNECTED.equals(e.getValue().getStatus()))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Close all connections gracefully (FP-friendly, ignores exceptions per connection).
     */
    public static void closeAll(final Map<String, Connection> connections) {
        if (Objects.isNull(connections)) {
            return;
        }
        connections.entrySet().stream()
                .filter(e -> Objects.nonNull(e.getValue()))
                .peek(e -> {
                    try {
                        e.getValue().drain(Duration.ofSeconds(2));
                    } catch (final Exception ex) {
                        log.warn("Failed to drain NATS connection [{}]", e.getKey(), ex);
                    }
                })
                .forEach(e -> {
                    try {
                        e.getValue().close();
                    } catch (final Exception ex) {
                        log.warn("Failed to close NATS connection [{}]", e.getKey(), ex);
                    }
                });
    }

    private static Optional<Connection> establishOne(final ServerSpec spec) {
        try {
            final Options options = Options.builder()
                    .servers(spec.urls().stream()
                            .filter(Objects::nonNull)
                            .filter(s -> !s.isBlank())
                            .toArray(String[]::new))
                    .connectionTimeout(DEFAULT_CONNECT_TIMEOUT)
                    .connectionListener(loggingListener(spec.name()))
                    .maxReconnects(-1)
                    .reconnectWait(Duration.ofSeconds(1))
                    .errorListener(new io.nats.client.ErrorListener() {
                        @Override
                        public void errorOccurred(final Connection conn, final String error) {
                            log.warn("NATS [{}] error: {}", spec.name(), error);
                        }
                    })
                    .build();
            final Connection conn = Nats.connect(options);
            log.info("NATS [{}] connected via {}", spec.name(), spec.urls());
            return Optional.of(conn);
        } catch (final Exception ex) {
            log.error("Failed to connect to NATS [{}] at {}", spec.name(), spec.urls(), ex);
            return Optional.empty();
        }
    }
}
