package com.enterprise.rag.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Typed configuration properties for the application.
 * Bound from {@code app.*} in application.yml.
 */
@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Rag rag = new Rag();
    private Storage storage = new Storage();
    private Security security = new Security();

    @Data
    public static class Rag {
        private int topK = 5;
        private double similarityThreshold = 0.65;
        private int chunkSize = 512;
        private int chunkOverlap = 64;
        private int maxContextChars = 8000;
        private int maxConversationMessages = 10;
    }

    @Data
    public static class Storage {
        private String uploadDir = "./uploads";
    }

    @Data
    public static class Security {
        private Cors cors = new Cors();

        @Data
        public static class Cors {
            private List<String> allowedOrigins = List.of("http://localhost:5173");
        }
    }
}
