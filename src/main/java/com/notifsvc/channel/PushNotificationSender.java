package com.notifsvc.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PushNotificationSender extends AbstractSimulatedSender {

    public PushNotificationSender(
            @Value("${notifsvc.senders.push.failure-rate:0.1}") double failureRate,
            @Value("${notifsvc.senders.push.latency-millis:15}") long latencyMillis) {
        super(failureRate, latencyMillis);
    }

    @Override
    public ChannelType supportedChannel() {
        return ChannelType.PUSH;
    }
}
