package com.notifsvc.notification;

import com.notifsvc.channel.ChannelType;
import com.notifsvc.common.ConflictException;
import com.notifsvc.common.NotFoundException;
import com.notifsvc.delivery.DeliveryAttempt;
import com.notifsvc.delivery.DeliveryAttemptRepository;
import com.notifsvc.template.Template;
import com.notifsvc.template.TemplateEngine;
import com.notifsvc.template.TemplateRepository;
import com.notifsvc.tenant.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class NotificationService {

    private final NotificationRequestRepository notificationRequestRepository;
    private final TemplateRepository templateRepository;
    private final TenantRepository tenantRepository;
    private final TemplateEngine templateEngine;
    private final DeliveryAttemptRepository deliveryAttemptRepository;

    public NotificationService(
            NotificationRequestRepository notificationRequestRepository,
            TemplateRepository templateRepository,
            TenantRepository tenantRepository,
            TemplateEngine templateEngine,
            DeliveryAttemptRepository deliveryAttemptRepository) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.templateRepository = templateRepository;
        this.tenantRepository = tenantRepository;
        this.templateEngine = templateEngine;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
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

    public List<NotificationRequest> search(Long tenantId, NotificationStatus status, ChannelType channelType) {
        return notificationRequestRepository.search(tenantId, status, channelType);
    }

    public DeliveryReportResponse deliveryReport(Long tenantId) {
        Map<NotificationStatus, Long> byStatus = new EnumMap<>(NotificationStatus.class);
        long total = 0;
        for (StatusCount sc : notificationRequestRepository.countByStatusForTenant(tenantId)) {
            byStatus.put(sc.status(), sc.count());
            total += sc.count();
        }

        Map<ChannelType, Long> byChannel = new EnumMap<>(ChannelType.class);
        for (ChannelCount cc : notificationRequestRepository.countByChannelForTenant(tenantId)) {
            byChannel.put(cc.channelType(), cc.count());
        }

        long delivered = byStatus.getOrDefault(NotificationStatus.DELIVERED, 0L) + byStatus.getOrDefault(NotificationStatus.SENT, 0L);
        double deliveryRate = total == 0 ? 0.0 : (double) delivered / total;

        return new DeliveryReportResponse(total, byStatus, byChannel, deliveryRate);
    }

    public List<DeliveryAttempt> attemptsForNotification(Long tenantId, Long notificationId) {
        getForTenant(tenantId, notificationId); // ensures the notification belongs to this tenant
        return deliveryAttemptRepository.findByNotificationIdOrderByAttemptNumberAsc(notificationId);
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
