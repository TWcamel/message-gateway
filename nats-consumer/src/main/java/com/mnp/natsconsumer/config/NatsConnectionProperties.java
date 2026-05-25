package com.mnp.natsconsumer.config;

import com.mnp.commons.util.NatsUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Multi-server NATS configuration identical in shape to nats-adapter.
 * Each server exposes an isolated INBOX subject for this consumer.
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "mnp.nats")
public class NatsConnectionProperties {

    private List<ServerSpec> servers;

    @Data
    public static class ServerSpec {
        private String name;
        private List<String> urls;
        private String inboxSubject;
        private String outboxSubject;

        public NatsUtils.ServerSpec toCommons() {
            return new NatsUtils.ServerSpec(name, urls, inboxSubject, outboxSubject);
        }
    }

    @Bean
    public List<NatsUtils.ServerSpec> natsServerSpecs() {
        log.info("Consumer configured NATS servers: {}", servers.stream()
                .map(ServerSpec::getName)
                .collect(Collectors.toList()));
        return servers.stream()
                .map(ServerSpec::toCommons)
                .collect(Collectors.toList());
    }
}
