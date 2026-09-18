package com.enterprise.rag.ai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures the primary Spring AI {@link ChatClient}.
 *
 * <p>The RAG orchestrator adds advisors at query time (QuestionAnswerAdvisor).
 * This factory provides the base client with structured logging.
 *
 * <p>The LLM model is configured via {@code spring.ai.ollama.chat.model} in application.yml.
 * Business logic depends only on {@link ChatClient} — never on Ollama-specific APIs.
 */
@Configuration
public class ChatClientConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
}
