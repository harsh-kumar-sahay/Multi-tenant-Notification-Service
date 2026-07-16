package com.notifsvc.channel;

import com.notifsvc.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/channels")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class ChannelConfigController {

    private final ChannelConfigService channelConfigService;

    public ChannelConfigController(ChannelConfigService channelConfigService) {
        this.channelConfigService = channelConfigService;
    }

    @PutMapping
    public ResponseEntity<ChannelConfigResponse> upsert(@Valid @RequestBody ChannelConfigRequest request) {
        Long tenantId = CurrentUser.get().getTenantId();
        ChannelConfig config = channelConfigService.upsert(tenantId, request);
        return ResponseEntity.ok(ChannelConfigResponse.from(config));
    }

    @GetMapping
    public List<ChannelConfigResponse> list() {
        Long tenantId = CurrentUser.get().getTenantId();
        return channelConfigService.listForTenant(tenantId).stream().map(ChannelConfigResponse::from).toList();
    }
}
