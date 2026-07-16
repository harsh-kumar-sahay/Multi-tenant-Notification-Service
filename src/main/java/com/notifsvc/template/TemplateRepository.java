package com.notifsvc.template;

import com.notifsvc.channel.ChannelType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByTenantId(Long tenantId);
    List<Template> findByTenantIdAndChannelType(Long tenantId, ChannelType channelType);
    Optional<Template> findTopByTenantIdAndNameOrderByVersionDesc(Long tenantId, String name);
    Optional<Template> findByIdAndTenantId(Long id, Long tenantId);
}
