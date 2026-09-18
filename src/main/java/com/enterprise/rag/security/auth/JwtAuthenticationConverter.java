package com.enterprise.rag.security.auth;

import com.enterprise.rag.security.tenant.TenantAwareAuthenticationToken;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Converts a validated Keycloak JWT into a TenantAwareAuthenticationToken.
 *
 * Role extraction order:
 *   1. resource_access.<clientId>.roles  (client-level roles — preferred)
 *   2. realm_access.roles                (realm-level roles — fallback)
 *
 * tenant_id is read from the custom JWT claim configured via Keycloak Protocol Mapper.
 */
@Component
public class JwtAuthenticationConverter implements Converter<Jwt, TenantAwareAuthenticationToken> {

    private static final String TENANT_CLAIM  = "tenant_id";
    private static final String CLIENT_ID     = "rag-backend";
    private static final String RESOURCE_ACCESS = "resource_access";
    private static final String REALM_ACCESS  = "realm_access";
    private static final String ROLES         = "roles";

    @Override
    public TenantAwareAuthenticationToken convert(Jwt jwt) {
        String tenantId = jwt.getClaimAsString(TENANT_CLAIM);
        Collection<GrantedAuthority> authorities = extractAuthorities(jwt);
        return new TenantAwareAuthenticationToken(jwt, tenantId, authorities);
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        List<GrantedAuthority> authorities = new ArrayList<>();

        // 1. Client-level roles (preferred)
        Map<String, Object> resourceAccess = jwt.getClaim(RESOURCE_ACCESS);
        if (resourceAccess != null && resourceAccess.containsKey(CLIENT_ID)) {
            Map<String, Object> clientAccess = (Map<String, Object>) resourceAccess.get(CLIENT_ID);
            List<String> clientRoles = (List<String>) clientAccess.get(ROLES);
            if (clientRoles != null) {
                clientRoles.stream()
                        .map(role -> new SimpleGrantedAuthority(normaliseRole(role)))
                        .forEach(authorities::add);
            }
        }

        // 2. Realm-level roles (fallback)
        if (authorities.isEmpty()) {
            Map<String, Object> realmAccess = jwt.getClaim(REALM_ACCESS);
            if (realmAccess != null) {
                List<String> realmRoles = (List<String>) realmAccess.get(ROLES);
                if (realmRoles != null) {
                    realmRoles.stream()
                            .map(role -> new SimpleGrantedAuthority(normaliseRole(role)))
                            .forEach(authorities::add);
                }
            }
        }

        return authorities;
    }

    /** Ensures roles are always prefixed with ROLE_ for Spring Security hasRole() compatibility. */
    private String normaliseRole(String role) {
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
