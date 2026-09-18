package com.enterprise.rag.security;

import com.enterprise.rag.security.authentication.KeycloakJwtAuthenticationConverter;
import com.enterprise.rag.security.tenant.TenantContextFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration.
 *
 * <ul>
 *   <li>Stateless JWT resource server backed by Keycloak</li>
 *   <li>All {@code /api/v1/**} endpoints require authentication</li>
 *   <li>MCP SSE endpoint requires authentication</li>
 *   <li>Actuator health/info are public; all other actuator endpoints require ADMIN role</li>
 *   <li>{@link TenantContextFilter} populates {@link com.enterprise.rag.security.tenant.TenantContext}
 *       after JWT validation</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final KeycloakJwtAuthenticationConverter jwtConverter;
    private final TenantContextFilter tenantContextFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm ->
                sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public: health probes and API docs
                .requestMatchers("/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/api-docs/**").permitAll()
                // Actuator management requires ADMIN
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                // Document upload requires UPLOADER or ADMIN
                .requestMatchers(HttpMethod.POST, "/api/v1/documents").hasAnyRole("UPLOADER", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/v1/documents/**").hasAnyRole("UPLOADER", "ADMIN")
                // All other API endpoints require any authenticated user
                .requestMatchers("/api/v1/**").authenticated()
                // MCP SSE endpoint
                .requestMatchers("/mcp/**").authenticated()
                .anyRequest().denyAll()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter))
            )
            // TenantContextFilter must run AFTER BearerTokenAuthenticationFilter
            // so the SecurityContext already contains the validated JWT principal
            .addFilterAfter(tenantContextFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }
}
