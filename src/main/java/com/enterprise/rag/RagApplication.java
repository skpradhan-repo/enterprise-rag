package com.enterprise.rag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enterprise RAG Platform – application entry point.
 *
 * <p>Profiles:
 * <ul>
 *   <li>{@code default} — full web server with RAG, MCP client, security</li>
 *   <li>{@code local}   — overrides for developer laptop (relaxed CORS, h2 console etc.)</li>
 *   <li>{@code test}    — Testcontainers-based integration test profile</li>
 * </ul>
 */
@SpringBootApplication
@EnableAsync
@ConfigurationPropertiesScan
public class RagApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagApplication.class, args);
    }
}
