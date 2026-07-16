package com.notifsvc.channel;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChannelConfigRepository extends JpaRepository<ChannelConfig, Long> {
    List<ChannelConfig> findByTenantId(Long tenantId);
    Optional<ChannelConfig> findByTenantIdAndChannelType(Long tenantId, ChannelType channelType);
}
