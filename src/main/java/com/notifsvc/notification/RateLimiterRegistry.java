package com.notifsvc.notification;

import com.notifsvc.tenant.TenantRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterRegistry {

    private static final int DEFAULT_MAX_PER_MINUTE = 60;

    private final TenantRepository tenantRepository;
    private final Map<Long, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterRegistry(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public boolean tryAcquire(Long tenantId) {
        TokenBucket bucket = buckets.computeIfAbsent(tenantId, id -> new TokenBucket(resolveMaxPerMinute(id)));
        return bucket.tryAcquire();
    }

    /** Call after a tenant's rate limit changes so the next acquire picks up the new rate. */
    public void invalidate(Long tenantId) {
        buckets.remove(tenantId);
    }

    private int resolveMaxPerMinute(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .map(t -> t.getGlobalRateLimitPerMinute() != null ? t.getGlobalRateLimitPerMinute() : DEFAULT_MAX_PER_MINUTE)
                .orElse(DEFAULT_MAX_PER_MINUTE);
    }
}
