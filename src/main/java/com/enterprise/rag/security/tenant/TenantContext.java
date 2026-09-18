package com.enterprise.rag.security.tenant;

/**
 * ThreadLocal holder for the current request's tenant slug.
 * Populated by {@link TenantContextFilter} from the JWT {@code tenant_id} claim.
 * Always call {@link #clear()} in a finally block to prevent leaks.
 */
public final class TenantContext {

    private static final ThreadLocal<String> TENANT = new ThreadLocal<>();

    private TenantContext() {}

    public static void set(String tenantSlug) {
        TENANT.set(tenantSlug);
    }

    public static String get() {
        return TENANT.get();
    }

    public static void clear() {
        TENANT.remove();
    }

    public static boolean isSet() {
        return TENANT.get() != null;
    }
}
