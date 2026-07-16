package com.notifsvc.notification;

import java.util.Map;

public record DeliveryReportResponse(
        long total,
        Map<NotificationStatus, Long> byStatus,
        Map<com.notifsvc.channel.ChannelType, Long> byChannel,
        double deliveryRate
) {
}
