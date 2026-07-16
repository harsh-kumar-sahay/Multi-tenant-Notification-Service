package com.notifsvc.user;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/admins")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class TenantAdminController {

    private final AppUserService appUserService;

    public TenantAdminController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @PostMapping
    public ResponseEntity<AppUserResponse> create(@PathVariable Long tenantId, @Valid @RequestBody CreateTenantAdminRequest request) {
        AppUser user = appUserService.createTenantAdmin(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(AppUserResponse.from(user));
    }

    @GetMapping
    public List<AppUserResponse> list(@PathVariable Long tenantId) {
        return appUserService.listForTenant(tenantId).stream().map(AppUserResponse::from).toList();
    }
}
