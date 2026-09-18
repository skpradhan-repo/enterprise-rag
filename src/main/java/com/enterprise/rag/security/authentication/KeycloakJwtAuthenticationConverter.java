package com.enterprise.rag.security.authentication;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Converts a Keycloak JWT into a Spring Security {@link JwtAuthenticationToken}.
 *
 * <p>Keycloak places realm roles under {@code realm_access.roles} and client roles
 * under {@code resource_access.<client-id>.roles}. Both are extracted and prefixed
 * with {@code ROLE_} so Spring Security {@code @PreAuthorize("hasRole(...)")} works.
 *
 * <p>The {@code tenant_id} claim is extracted and stored in the principal name for
 * downstream use by {@link com.enterprise.rag.security.tenant.TenantContextFilter}.
 */
@Component
public class KeycloakJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private static final String REALM_ACCESS    = "realm_access";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String ROLES           = "roles";
    private static final String ROLE_PREFIX     = "ROLE_";

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // ── Realm-level roles ─────────────────────────────────────────────
        Map<String, Object> realmAccess = jwt.getClaimAsMap(REALM_ACCESS);
        if (realmAccess != null) {
            Object realmRoles = realmAccess.get(ROLES);
            if (realmRoles instanceof List<?> roleList) {
                roleList.stream()
                        .filter(String.class::isInstance)
                        .map(r -> new SimpleGrantedAuthority(ROLE_PREFIX + r))
                        .forEach(authorities::add);
            }
        }

        // ── Client-level roles ────────────────────────────────────────────
        Map<String, Object> resourceAccess = jwt.getClaimAsMap(RESOURCE_ACCESS);
        if (resourceAccess != null) {
            resourceAccess.forEach((clientId, value) -> {
                if (value instanceof Map<?, ?> clientRoles) {
                    Object clientRoleList = clientRoles.get(ROLES);
                    if (clientRoleList instanceof List<?> roleList) {
                        roleList.stream()
                                .filter(String.class::isInstance)
                                .map(r -> new SimpleGrantedAuthority(ROLE_PREFIX + r))
                                .forEach(authorities::add);
                    }
                }
            });
        }

        return authorities;
    }
}
