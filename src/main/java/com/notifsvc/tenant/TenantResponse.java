package com.notifsvc.tenant;

import java.time.Instant;

public record TenantResponse(
        Long id,
        String name,
        TenantStatus status,
        Integer globalRateLimitPerMinute,
        Instant createdAt
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getGlobalRateLimitPerMinute(),
                tenant.getCreatedAt());
    }
}
