package com.enterprise.rag.security.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Enables @PreAuthorize, @PostAuthorize, and @Secured annotations.
 * prePostEnabled=true is the default but explicitly declared for clarity.
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true, securedEnabled = true)
public class MethodSecurityConfig {
}
