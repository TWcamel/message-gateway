package com.mnp.gcpmnp.controller;

import com.mnp.commons.api.ApiResponse;
import com.mnp.commons.util.IdUtils;
import com.mnp.commons.util.TimeUtils;
import com.mnp.gcpmnp.service.OutboxPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * OUTBOX entry: HTTP → Pub/Sub.
 * <p>Uses snowflake messageId; response follows {code:MNPGCP200,data:{messageId}} template.</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublishController {

    private final OutboxPublisher outboxPublisher;

    @PostMapping("/publish")
    public CompletableFuture<ApiResponse<Map<String, String>>> publish() {
        final String messageId = IdUtils.nextId();
        final Map<String, String> payload = Map.of(
                "messageId", messageId,
                "publishedAt", TimeUtils.toIsoString(java.time.Instant.now())
        );
        log.info("Received publish request messageId={}", messageId);

        return outboxPublisher.publish(payload)
                .thenApply(gcpId -> ApiResponse.success(Map.of("messageId", messageId, "gcpMessageId", gcpId)))
                .exceptionally(ex -> {
                    final Throwable cause = (ex instanceof CompletionException cex) ? cex.getCause() : ex;
                    log.error("Publish failed messageId={}", messageId, cause);
                    return ApiResponse.error(cause == null ? "unknown" : cause.getMessage());
                });
    }
}
