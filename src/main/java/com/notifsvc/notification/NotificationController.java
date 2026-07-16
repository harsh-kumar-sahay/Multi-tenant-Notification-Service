package com.notifsvc.notification;

import com.notifsvc.auth.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<NotificationResponse> create(@Valid @RequestBody NotificationCreateRequest request) {
        Long tenantId = CurrentUser.get().getTenantId();
        var result = notificationService.create(tenantId, request);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(NotificationResponse.from(result.notification()));
    }

    @GetMapping
    public List<NotificationResponse> list() {
        Long tenantId = CurrentUser.get().getTenantId();
        return notificationService.listForTenant(tenantId).stream().map(NotificationResponse::from).toList();
    }

    @GetMapping("/{id}")
    public NotificationResponse getById(@PathVariable Long id) {
        Long tenantId = CurrentUser.get().getTenantId();
        return NotificationResponse.from(notificationService.getForTenant(tenantId, id));
    }

    @PatchMapping("/{id}/cancel")
    public NotificationResponse cancel(@PathVariable Long id) {
        Long tenantId = CurrentUser.get().getTenantId();
        return NotificationResponse.from(notificationService.cancel(tenantId, id));
    }
}
