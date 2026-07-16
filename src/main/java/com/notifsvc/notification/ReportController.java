package com.notifsvc.notification;

import com.notifsvc.auth.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class ReportController {

    private final NotificationService notificationService;

    public ReportController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/delivery")
    public DeliveryReportResponse deliveryReport() {
        Long tenantId = CurrentUser.get().getTenantId();
        return notificationService.deliveryReport(tenantId);
    }
}
