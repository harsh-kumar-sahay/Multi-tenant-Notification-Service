package com.notifsvc.tenant;

import com.notifsvc.common.ConflictException;
import com.notifsvc.common.NotFoundException;
import com.notifsvc.notification.RateLimiterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final RateLimiterRegistry rateLimiterRegistry;

    public TenantService(TenantRepository tenantRepository, RateLimiterRegistry rateLimiterRegistry) {
        this.tenantRepository = tenantRepository;
        this.rateLimiterRegistry = rateLimiterRegistry;
    }

    @Transactional
    public Tenant create(TenantCreateRequest request) {
        if (tenantRepository.existsByName(request.name())) {
            throw new ConflictException("A tenant named '" + request.name() + "' already exists");
        }
        Tenant tenant = new Tenant();
        tenant.setName(request.name());
        tenant.setGlobalRateLimitPerMinute(request.globalRateLimitPerMinute());
        return tenantRepository.save(tenant);
    }

    public List<Tenant> listAll() {
        return tenantRepository.findAll();
    }

    public Tenant getById(Long id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No tenant with id " + id));
    }

    @Transactional
    public Tenant update(Long id, TenantUpdateRequest request) {
        Tenant tenant = getById(id);
        if (request.status() != null) {
            tenant.setStatus(request.status());
        }
        if (request.globalRateLimitPerMinute() != null
                && !Objects.equals(request.globalRateLimitPerMinute(), tenant.getGlobalRateLimitPerMinute())) {
            tenant.setGlobalRateLimitPerMinute(request.globalRateLimitPerMinute());
            rateLimiterRegistry.invalidate(id);
        }
        return tenantRepository.save(tenant);
    }
}
