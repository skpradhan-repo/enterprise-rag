package com.enterprise.rag.security.authorization;

import com.enterprise.rag.security.tenant.TenantContext;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;

/**
 * Builds PGVector {@link Filter.Expression} objects that enforce tenant isolation
 * and document authorization during vector similarity search.
 *
 * <p>The metadata stored on each vector chunk contains:
 * <ul>
 *   <li>{@code tenant_id} — the tenant slug the document belongs to</li>
 *   <li>{@code status}    — must be {@code INDEXED}</li>
 * </ul>
 *
 * <p>All retrieval calls MUST pass the filter returned by
 * {@link #buildTenantFilter()} or {@link #buildTenantAndStatusFilter()}.
 * Skipping this filter would allow cross-tenant data leakage.
 */
@Component
public class MetadataFilterBuilder {

    private static final String TENANT_ID_KEY = "tenant_id";
    private static final String STATUS_KEY    = "status";
    private static final String INDEXED       = "INDEXED";

    /**
     * Builds a filter restricting retrieval to the current tenant only.
     *
     * @throws IllegalStateException if no tenant is set in {@link TenantContext}
     */
    public Filter.Expression buildTenantFilter() {
        String tenantId = requireTenant();
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        return b.eq(TENANT_ID_KEY, tenantId).build();
    }

    /**
     * Builds a filter restricting to current tenant AND only INDEXED documents.
     * This is the filter that should be used for all production RAG queries.
     */
    public Filter.Expression buildTenantAndStatusFilter() {
        String tenantId = requireTenant();
        FilterExpressionBuilder b = new FilterExpressionBuilder();
        return b.and(
                b.eq(TENANT_ID_KEY, tenantId),
                b.eq(STATUS_KEY, INDEXED)
        ).build();
    }

    private String requireTenant() {
        String tenant = TenantContext.get();
        if (tenant == null || tenant.isBlank()) {
            throw new IllegalStateException(
                    "No tenant context set. All vector retrievals require an authenticated tenant.");
        }
        return tenant;
    }
}
