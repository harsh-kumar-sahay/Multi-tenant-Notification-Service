package com.notifsvc.notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRequestRepository extends JpaRepository<NotificationRequest, Long> {

    Optional<NotificationRequest> findByIdAndTenantId(Long id, Long tenantId);

    List<NotificationRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    boolean existsByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

    Optional<NotificationRequest> findByTenantIdAndIdempotencyKey(Long tenantId, String idempotencyKey);

    /**
     * Fetch up to `limit` ready rows for one tenant, locking them so no other worker/poller
     * thread can claim the same rows concurrently. SKIP LOCKED means a busy row is simply
     * skipped rather than blocking this query.
     */
    @Query(value = """
            SELECT * FROM notification_requests
            WHERE tenant_id = :tenantId
              AND status IN ('PENDING', 'RETRY_SCHEDULED')
              AND scheduled_at <= :now
              AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            ORDER BY scheduled_at ASC
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<NotificationRequest> claimReadyForTenant(@Param("tenantId") Long tenantId,
                                                   @Param("now") Instant now,
                                                   @Param("limit") int limit);

    @Query("SELECT DISTINCT n.tenant.id FROM NotificationRequest n WHERE n.status IN ('PENDING', 'RETRY_SCHEDULED') AND n.scheduledAt <= :now")
    List<Long> findTenantIdsWithReadyWork(@Param("now") Instant now);

    @Query("""
            SELECT n FROM NotificationRequest n
            WHERE n.tenant.id = :tenantId
              AND (:status IS NULL OR n.status = :status)
              AND (:channelType IS NULL OR n.channelType = :channelType)
            ORDER BY n.createdAt DESC
            """)
    List<NotificationRequest> search(@Param("tenantId") Long tenantId,
                                      @Param("status") NotificationStatus status,
                                      @Param("channelType") com.notifsvc.channel.ChannelType channelType);

    @Query("""
            SELECT new com.notifsvc.notification.StatusCount(n.status, COUNT(n))
            FROM NotificationRequest n WHERE n.tenant.id = :tenantId GROUP BY n.status
            """)
    List<StatusCount> countByStatusForTenant(@Param("tenantId") Long tenantId);

    @Query("""
            SELECT new com.notifsvc.notification.ChannelCount(n.channelType, COUNT(n))
            FROM NotificationRequest n WHERE n.tenant.id = :tenantId GROUP BY n.channelType
            """)
    List<ChannelCount> countByChannelForTenant(@Param("tenantId") Long tenantId);
}
