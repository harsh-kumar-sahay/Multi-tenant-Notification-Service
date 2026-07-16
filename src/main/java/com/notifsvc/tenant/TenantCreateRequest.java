package com.notifsvc.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TenantCreateRequest(
        @NotBlank String name,
        @Positive Integer globalRateLimitPerMinute
) {
}
