package com.notifsvc.tenant;

import jakarta.validation.constraints.Positive;

public record TenantUpdateRequest(
        TenantStatus status,
        @Positive Integer globalRateLimitPerMinute
) {
}
