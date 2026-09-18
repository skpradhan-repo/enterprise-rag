package com.enterprise.rag.security.authorization;

import com.enterprise.rag.security.tenant.TenantContext;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Programmatic authorization helper used by service and MCP tool layers.
 *
 * <p>All tenant and role assertions are server-side only.
 * Client-provided tenant IDs or roles are never trusted.
 */
@Component
public class RagAuthorizationService {

    /**
     * Asserts that the current user belongs to the expected tenant.
     * Throws {@link AccessDeniedException} on mismatch — never reveals which tenant was expected.
     */
    public void assertTenantAccess(String requiredTenantId) {
        String currentTenant = TenantContext.get();
        if (currentTenant == null || !currentTenant.equals(requiredTenantId)) {
            throw new AccessDeniedException("Access denied.");
        }
    }

    /**
     * Returns the tenant ID from the current JWT without trusting any client input.
     */
    public String getCurrentTenantId() {
        Jwt jwt = getJwt();
        String tenantId = jwt.getClaimAsString("tenant_id");
        if (tenantId == null || tenantId.isBlank()) {
            throw new AccessDeniedException("No tenant_id claim in token.");
        }
        return tenantId;
    }

    /**
     * Returns the Keycloak subject (user ID) from the current JWT.
     */
    public String getCurrentUserId() {
        return getJwt().getSubject();
    }

    /**
     * Returns true if the current user has the given realm or client role.
     */
    public boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }

    private Jwt getJwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Jwt jwt)) {
            throw new AccessDeniedException("No authenticated JWT principal.");
        }
        return jwt;
    }
}
