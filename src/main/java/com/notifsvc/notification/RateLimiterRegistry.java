package com.notifsvc.notification;

import com.notifsvc.channel.ChannelType;
import com.notifsvc.tenant.TenantRepository;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimiterRegistry {

    private static final int DEFAULT_MAX_PER_MINUTE = 60;

    private final RateLimitPolicyRepository rateLimitPolicyRepository;
    private final TenantRepository tenantRepository;
    private final Map<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiterRegistry(RateLimitPolicyRepository rateLimitPolicyRepository, TenantRepository tenantRepository) {
        this.rateLimitPolicyRepository = rateLimitPolicyRepository;
        this.tenantRepository = tenantRepository;
    }

    public boolean tryAcquire(Long tenantId, ChannelType channelType) {
        String key = tenantId + ":" + channelType;
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(resolveMaxPerMinute(tenantId, channelType)));
        return bucket.tryAcquire();
    }

    /** Call after a policy or tenant limit change so the next acquire picks up the new rate. */
    public void invalidate(Long tenantId, ChannelType channelType) {
        buckets.remove(tenantId + ":" + channelType);
    }

    private int resolveMaxPerMinute(Long tenantId, ChannelType channelType) {
        return rateLimitPolicyRepository.findByTenantIdAndChannelType(tenantId, channelType)
                .map(RateLimitPolicy::getMaxPerMinute)
                .orElseGet(() -> tenantRepository.findById(tenantId)
                        .map(t -> t.getGlobalRateLimitPerMinute() != null ? t.getGlobalRateLimitPerMinute() : DEFAULT_MAX_PER_MINUTE)
                        .orElse(DEFAULT_MAX_PER_MINUTE));
    }
}
