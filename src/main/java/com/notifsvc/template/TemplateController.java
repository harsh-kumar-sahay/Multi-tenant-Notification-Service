package com.notifsvc.template;

import com.notifsvc.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/templates")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(@Valid @RequestBody TemplateCreateRequest request) {
        Long tenantId = CurrentUser.get().getTenantId();
        Template template = templateService.create(tenantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(TemplateResponse.from(template));
    }

    @PutMapping("/{id}/revise")
    public TemplateResponse revise(@PathVariable Long id, @Valid @RequestBody TemplateReviseRequest request) {
        Long tenantId = CurrentUser.get().getTenantId();
        return TemplateResponse.from(templateService.revise(tenantId, id, request));
    }

    @GetMapping
    public List<TemplateResponse> listActive() {
        Long tenantId = CurrentUser.get().getTenantId();
        return templateService.listActiveForTenant(tenantId).stream().map(TemplateResponse::from).toList();
    }

    @GetMapping("/{name}/history")
    public List<TemplateResponse> history(@PathVariable String name) {
        Long tenantId = CurrentUser.get().getTenantId();
        return templateService.listVersionHistory(tenantId, name).stream().map(TemplateResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TemplateResponse getById(@PathVariable Long id) {
        Long tenantId = CurrentUser.get().getTenantId();
        return TemplateResponse.from(templateService.getByIdForTenant(tenantId, id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        Long tenantId = CurrentUser.get().getTenantId();
        templateService.deactivate(tenantId, id);
        return ResponseEntity.noContent().build();
    }
}
