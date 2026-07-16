package com.notifsvc.notification;

import com.notifsvc.common.ConflictException;
import com.notifsvc.common.NotFoundException;
import com.notifsvc.template.Template;
import com.notifsvc.template.TemplateEngine;
import com.notifsvc.template.TemplateRepository;
import com.notifsvc.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRequestRepository notificationRequestRepository;
    private final TemplateRepository templateRepository;
    private final TenantRepository tenantRepository;
    private final TemplateEngine templateEngine;

    public NotificationService(
            NotificationRequestRepository notificationRequestRepository,
            TemplateRepository templateRepository,
            TenantRepository tenantRepository,
            TemplateEngine templateEngine) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.templateRepository = templateRepository;
        this.tenantRepository = tenantRepository;
        this.templateEngine = templateEngine;
    }

    public record CreateResult(NotificationRequest notification, boolean created) {
    }

    @Transactional
    public CreateResult create(Long tenantId, NotificationCreateRequest request) {
        var existing = notificationRequestRepository.findByTenantIdAndIdempotencyKey(tenantId, request.idempotencyKey());
        if (existing.isPresent()) {
            return new CreateResult(existing.get(), false);
        }

        Template template = templateRepository.findByIdAndTenantId(request.templateId(), tenantId)
                .orElseThrow(() -> new NotFoundException("No template with id " + request.templateId() + " for this tenant"));
        if (!template.isActive()) {
            throw new IllegalArgumentException("Template '" + template.getName() + "' is not active; use its latest active version");
        }

        Map<String, String> variables = request.variables() != null ? request.variables() : new HashMap<>();
        // Fail fast on missing variables rather than discovering it at dispatch time.
        templateEngine.render(template.getBody(), variables);
        if (template.getSubject() != null) {
            templateEngine.render(template.getSubject(), variables);
        }

        NotificationRequest notification = new NotificationRequest();
        notification.setTenant(tenantRepository.getReferenceById(tenantId));
        notification.setTemplate(template);
        notification.setChannelType(template.getChannelType());
        notification.setRecipient(request.recipient());
        notification.setVariables(variables);
        notification.setIdempotencyKey(request.idempotencyKey());
        notification.setScheduledAt(request.scheduledAt() != null ? request.scheduledAt() : Instant.now());
        if (request.maxAttempts() != null) {
            notification.setMaxAttempts(request.maxAttempts());
        }
        notification.setStatus(NotificationStatus.PENDING);
        return new CreateResult(notificationRequestRepository.save(notification), true);
    }

    public NotificationRequest getForTenant(Long tenantId, Long id) {
        return notificationRequestRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new NotFoundException("No notification with id " + id + " for this tenant"));
    }

    public List<NotificationRequest> listForTenant(Long tenantId) {
        return notificationRequestRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
    }

    @Transactional
    public NotificationRequest cancel(Long tenantId, Long id) {
        NotificationRequest notification = getForTenant(tenantId, id);
        if (notification.getStatus() != NotificationStatus.PENDING && notification.getStatus() != NotificationStatus.RETRY_SCHEDULED) {
            throw new ConflictException("Cannot cancel a notification in status " + notification.getStatus());
        }
        notification.setStatus(NotificationStatus.CANCELLED);
        return notificationRequestRepository.save(notification);
    }
}
