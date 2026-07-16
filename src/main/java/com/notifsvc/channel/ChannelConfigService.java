package com.notifsvc.channel;

import com.notifsvc.common.NotFoundException;
import com.notifsvc.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChannelConfigService {

    private final ChannelConfigRepository channelConfigRepository;
    private final TenantRepository tenantRepository;

    public ChannelConfigService(ChannelConfigRepository channelConfigRepository, TenantRepository tenantRepository) {
        this.channelConfigRepository = channelConfigRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public ChannelConfig upsert(Long tenantId, ChannelConfigRequest request) {
        ChannelConfig config = channelConfigRepository.findByTenantIdAndChannelType(tenantId, request.channelType())
                .orElseGet(() -> {
                    ChannelConfig created = new ChannelConfig();
                    created.setTenant(tenantRepository.getReferenceById(tenantId));
                    created.setChannelType(request.channelType());
                    return created;
                });
        config.setEnabled(request.enabled());
        config.setSenderIdentity(request.senderIdentity());
        return channelConfigRepository.save(config);
    }

    public List<ChannelConfig> listForTenant(Long tenantId) {
        return channelConfigRepository.findByTenantId(tenantId);
    }

    public ChannelConfig getForTenant(Long tenantId, ChannelType channelType) {
        return channelConfigRepository.findByTenantIdAndChannelType(tenantId, channelType)
                .orElseThrow(() -> new NotFoundException("No " + channelType + " channel config for this tenant"));
    }
}
