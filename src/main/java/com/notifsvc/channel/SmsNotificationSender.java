package com.notifsvc.channel;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SmsNotificationSender extends AbstractSimulatedSender {

    public SmsNotificationSender(
            @Value("${notifsvc.senders.sms.failure-rate:0.15}") double failureRate,
            @Value("${notifsvc.senders.sms.latency-millis:30}") long latencyMillis) {
        super(failureRate, latencyMillis);
    }

    @Override
    public ChannelType supportedChannel() {
        return ChannelType.SMS;
    }
}
