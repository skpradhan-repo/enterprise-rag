package com.enterprise.rag.configuration;

import org.apache.tika.parser.AutoDetectParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TikaConfig {

    @Bean
    public AutoDetectParser autoDetectParser() {
        return new AutoDetectParser();
    }
}
