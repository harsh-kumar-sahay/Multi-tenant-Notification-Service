package com.notifsvc.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Polls for ready notifications and dispatches them. Iterates tenants round-robin
 * (one bounded batch per tenant per cycle) rather than draining one tenant's whole
 * backlog first, so a noisy tenant can't starve the others.
 */
@Component
public class NotificationPoller {

    private static final Logger log = LoggerFactory.getLogger(NotificationPoller.class);

    private final NotificationRequestRepository notificationRequestRepository;
    private final NotificationDispatchService dispatchService;
    private final ChannelWorkerPools channelWorkerPools;
    private final int batchSizePerTenant;
    private final String workerId = "worker-" + UUID.randomUUID();

    public NotificationPoller(
            NotificationRequestRepository notificationRequestRepository,
            NotificationDispatchService dispatchService,
            ChannelWorkerPools channelWorkerPools,
            @Value("${notifsvc.dispatch.batch-size-per-tenant:10}") int batchSizePerTenant) {
        this.notificationRequestRepository = notificationRequestRepository;
        this.dispatchService = dispatchService;
        this.channelWorkerPools = channelWorkerPools;
        this.batchSizePerTenant = batchSizePerTenant;
    }

    @Scheduled(fixedDelayString = "${notifsvc.dispatch.poll-interval-millis:1000}")
    public void pollAndDispatch() {
        List<Long> tenantIds = notificationRequestRepository.findTenantIdsWithReadyWork(Instant.now());
        for (Long tenantId : tenantIds) {
            try {
                List<NotificationDispatchService.ClaimedNotification> claimed =
                        dispatchService.claimAndMarkSending(tenantId, batchSizePerTenant, workerId);
                for (var item : claimed) {
                    channelWorkerPools.submit(item.channelType(), () -> dispatchService.dispatchOne(item.id(), workerId));
                }
            } catch (Exception ex) {
                log.error("Error while polling/dispatching for tenant {}", tenantId, ex);
            }
        }
    }
}
