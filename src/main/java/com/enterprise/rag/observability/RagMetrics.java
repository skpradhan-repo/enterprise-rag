package com.enterprise.rag.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom Micrometer metrics for the RAG platform.
 *
 * <p>Tracks:
 * <ul>
 *   <li>{@code rag.chat.requests.total} — total successful RAG queries</li>
 *   <li>{@code rag.chat.failures.total} — total failed RAG queries</li>
 *   <li>{@code rag.chat.latency} — RAG query end-to-end latency</li>
 *   <li>{@code rag.ingestion.total} — total documents ingested</li>
 *   <li>{@code rag.ingestion.failures.total} — total failed ingestions</li>
 * </ul>
 */
@Component
public class RagMetrics {

    private final Counter chatSuccessCounter;
    private final Counter chatFailureCounter;
    private final Timer   chatLatencyTimer;
    private final Counter ingestionCounter;
    private final Counter ingestionFailureCounter;

    public RagMetrics(MeterRegistry registry) {
        this.chatSuccessCounter = Counter.builder("rag.chat.requests.total")
                .description("Total successful RAG chat requests")
                .register(registry);

        this.chatFailureCounter = Counter.builder("rag.chat.failures.total")
                .description("Total failed RAG chat requests")
                .register(registry);

        this.chatLatencyTimer = Timer.builder("rag.chat.latency")
                .description("RAG query end-to-end latency in milliseconds")
                .register(registry);

        this.ingestionCounter = Counter.builder("rag.ingestion.total")
                .description("Total documents successfully ingested")
                .register(registry);

        this.ingestionFailureCounter = Counter.builder("rag.ingestion.failures.total")
                .description("Total document ingestion failures")
                .register(registry);
    }

    public void recordChatSuccess(long latencyMs) {
        chatSuccessCounter.increment();
        chatLatencyTimer.record(latencyMs, TimeUnit.MILLISECONDS);
    }

    public void recordChatFailure() {
        chatFailureCounter.increment();
    }

    public void recordIngestionSuccess() {
        ingestionCounter.increment();
    }

    public void recordIngestionFailure() {
        ingestionFailureCounter.increment();
    }
}
