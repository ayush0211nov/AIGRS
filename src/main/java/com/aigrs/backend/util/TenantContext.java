package com.aigrs.backend.util;

/**
 * ThreadLocal holder for the current tenant's org ID.
 * Populated by JwtAuthenticationFilter on every authenticated request,
 * and cleared after the request completes.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_ORG_ID = new ThreadLocal<>();

    private TenantContext() {}

    public static String getOrgId() {
        return CURRENT_ORG_ID.get();
    }

    public static void setOrgId(String orgId) {
        CURRENT_ORG_ID.set(orgId);
    }

    public static void clear() {
        CURRENT_ORG_ID.remove();
    }

    public static java.util.UUID getOrgUUID() {
        String orgId = CURRENT_ORG_ID.get();
        return orgId != null ? java.util.UUID.fromString(orgId) : null;
    }
}
