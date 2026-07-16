package com.notifsvc.user;

import com.notifsvc.common.ConflictException;
import com.notifsvc.tenant.Tenant;
import com.notifsvc.tenant.TenantService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final TenantService tenantService;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(AppUserRepository appUserRepository, TenantService tenantService, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.tenantService = tenantService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser createTenantAdmin(Long tenantId, CreateTenantAdminRequest request) {
        Tenant tenant = tenantService.getById(tenantId);
        if (appUserRepository.existsByUsername(request.username())) {
            throw new ConflictException("Username '" + request.username() + "' is already taken");
        }
        AppUser user = new AppUser();
        user.setUsername(request.username());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(Role.TENANT_ADMIN);
        user.setTenant(tenant);
        return appUserRepository.save(user);
    }

    public List<AppUser> listForTenant(Long tenantId) {
        return appUserRepository.findByTenantId(tenantId);
    }
}
