package com.notifsvc.notification;

import com.notifsvc.channel.NotificationSender;
import com.notifsvc.channel.SendResult;
import com.notifsvc.channel.SenderRegistry;
import com.notifsvc.delivery.AttemptStatus;
import com.notifsvc.delivery.DeliveryAttempt;
import com.notifsvc.delivery.DeliveryAttemptRepository;
import com.notifsvc.template.TemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class NotificationDispatchService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDispatchService.class);

    private final NotificationRequestRepository notificationRequestRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final RateLimiterRegistry rateLimiterRegistry;
    private final BackoffCalculator backoffCalculator;
    private final SenderRegistry senderRegistry;
    private final TemplateEngine templateEngine;

    public NotificationDispatchService(
            NotificationRequestRepository notificationRequestRepository,
            DeliveryAttemptRepository deliveryAttemptRepository,
            RateLimiterRegistry rateLimiterRegistry,
            BackoffCalculator backoffCalculator,
            SenderRegistry senderRegistry,
            TemplateEngine templateEngine) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.deliveryAttemptRepository = deliveryAttemptRepository;
        this.rateLimiterRegistry = rateLimiterRegistry;
        this.backoffCalculator = backoffCalculator;
        this.senderRegistry = senderRegistry;
        this.templateEngine = templateEngine;
    }

    /**
     * Claims ready rows for one tenant (SKIP LOCKED under the hood), then for each claimed
     * row either marks it SENDING (rate limiter allowed) or leaves it untouched for the next
     * poll cycle (rate limiter rejected) - so a throttled notification never blocks a worker
     * thread, it just waits.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<ClaimedNotification> claimAndMarkSending(Long tenantId, int batchSize, String workerId) {
        List<NotificationRequest> claimed = notificationRequestRepository.claimReadyForTenant(tenantId, Instant.now(), batchSize);
        List<ClaimedNotification> toDispatch = new ArrayList<>();
        for (NotificationRequest notification : claimed) {
            if (!rateLimiterRegistry.tryAcquire(tenantId)) {
                continue;
            }
            notification.setStatus(NotificationStatus.SENDING);
            notification.setAttemptCount(notification.getAttemptCount() + 1);
            notificationRequestRepository.save(notification);
            toDispatch.add(new ClaimedNotification(notification.getId(), notification.getChannelType()));
        }
        return toDispatch;
    }

    public record ClaimedNotification(Long id, com.notifsvc.channel.ChannelType channelType) {
    }

    /** Runs the actual (simulated) send and records the outcome. Invoked on a channel worker thread. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void dispatchOne(Long notificationId, String workerId) {
        NotificationRequest notification = notificationRequestRepository.findById(notificationId).orElse(null);
        if (notification == null || notification.getStatus() != NotificationStatus.SENDING) {
            return;
        }

        DeliveryAttempt attempt = new DeliveryAttempt();
        attempt.setNotification(notification);
        attempt.setAttemptNumber(notification.getAttemptCount());
        attempt.setWorkerId(workerId);
        attempt.setStartedAt(Instant.now());

        try {
            String renderedBody = templateEngine.render(notification.getTemplate().getBody(), notification.getVariables());
            NotificationSender sender = senderRegistry.get(notification.getChannelType());
            SendResult result = sender.send(notification.getRecipient(), renderedBody);
            attempt.setCompletedAt(Instant.now());

            if (result.success()) {
                attempt.setStatus(AttemptStatus.SUCCESS);
                notification.setStatus(NotificationStatus.DELIVERED);
                notification.setNextAttemptAt(null);
            } else {
                attempt.setStatus(AttemptStatus.FAILURE);
                attempt.setErrorMessage(result.errorMessage());
                applyFailure(notification);
            }
        } catch (Exception ex) {
            log.warn("Dispatch attempt {} for notification {} threw", notification.getAttemptCount(), notificationId, ex);
            attempt.setCompletedAt(Instant.now());
            attempt.setStatus(AttemptStatus.FAILURE);
            attempt.setErrorMessage(ex.getMessage());
            applyFailure(notification);
        }

        deliveryAttemptRepository.save(attempt);
        notificationRequestRepository.save(notification);
    }

    private void applyFailure(NotificationRequest notification) {
        if (notification.getAttemptCount() >= notification.getMaxAttempts()) {
            notification.setStatus(NotificationStatus.DEAD_LETTER);
            notification.setNextAttemptAt(null);
        } else {
            notification.setStatus(NotificationStatus.RETRY_SCHEDULED);
            notification.setNextAttemptAt(backoffCalculator.nextAttemptAt(notification.getAttemptCount()));
        }
    }
}
