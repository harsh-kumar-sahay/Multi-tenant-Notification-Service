package com.notifsvc.tenant;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenants")
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    public ResponseEntity<TenantResponse> create(@Valid @RequestBody TenantCreateRequest request) {
        Tenant tenant = tenantService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(tenant));
    }

    @GetMapping
    public List<TenantResponse> listAll() {
        return tenantService.listAll().stream().map(TenantResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TenantResponse getById(@PathVariable Long id) {
        return TenantResponse.from(tenantService.getById(id));
    }

    @PatchMapping("/{id}")
    public TenantResponse update(@PathVariable Long id, @Valid @RequestBody TenantUpdateRequest request) {
        return TenantResponse.from(tenantService.update(id, request));
    }
}
