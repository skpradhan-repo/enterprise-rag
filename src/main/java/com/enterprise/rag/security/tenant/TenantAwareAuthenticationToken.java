package com.enterprise.rag.security.tenant;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * Custom authentication token that carries tenant_id alongside the standard JWT principal.
 * Created by JwtAuthenticationConverter after validating the JWT.
 */
public class TenantAwareAuthenticationToken extends AbstractAuthenticationToken {

    private final Jwt jwt;
    private final String tenantId;

    public TenantAwareAuthenticationToken(Jwt jwt,
                                          String tenantId,
                                          Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.jwt = jwt;
        this.tenantId = tenantId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return jwt.getTokenValue();
    }

    @Override
    public Object getPrincipal() {
        return jwt;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getSubject() {
        return jwt.getSubject();
    }

    public Jwt getJwt() {
        return jwt;
    }
}
