package com.notifsvc.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class InAppNotificationSender extends AbstractSimulatedSender {

    public InAppNotificationSender(
            @Value("${notifsvc.senders.in-app.failure-rate:0.02}") double failureRate,
            @Value("${notifsvc.senders.in-app.latency-millis:5}") long latencyMillis) {
        super(failureRate, latencyMillis);
    }

    @Override
    public ChannelType supportedChannel() {
        return ChannelType.IN_APP;
    }
}
