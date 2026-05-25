package com.mnp.natsadapter.config;

import com.mnp.commons.util.NatsUtils;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Multi-server NATS configuration.
 * <p>Each server gets an isolated inbox/outbox subject namespace.</p>
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

        /** Convert to commons ServerSpec (reuse). */
        public NatsUtils.ServerSpec toCommons() {
            return new NatsUtils.ServerSpec(name, urls, inboxSubject, outboxSubject);
        }
    }

    @Bean
    public List<NatsUtils.ServerSpec> natsServerSpecs() {
        log.info("Configured NATS servers: {}", servers.stream()
                .map(ServerSpec::getName)
                .collect(Collectors.toList()));
        return servers.stream()
                .map(ServerSpec::toCommons)
                .collect(Collectors.toList());
    }
}
