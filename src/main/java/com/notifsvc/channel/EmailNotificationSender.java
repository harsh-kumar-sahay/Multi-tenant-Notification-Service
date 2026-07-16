package com.notifsvc.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationSender extends AbstractSimulatedSender {

    public EmailNotificationSender(
            @Value("${notifsvc.senders.email.failure-rate:0.1}") double failureRate,
            @Value("${notifsvc.senders.email.latency-millis:20}") long latencyMillis) {
        super(failureRate, latencyMillis);
    }

    @Override
    public ChannelType supportedChannel() {
        return ChannelType.EMAIL;
    }
}
