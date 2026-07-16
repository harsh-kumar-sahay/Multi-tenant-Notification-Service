package com.notifsvc.notification;

import com.notifsvc.channel.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RateLimitPolicyRepository extends JpaRepository<RateLimitPolicy, Long> {
    List<RateLimitPolicy> findByTenantId(Long tenantId);
    Optional<RateLimitPolicy> findByTenantIdAndChannelType(Long tenantId, ChannelType channelType);
}
