package com.notifsvc.channel;

public interface NotificationSender {

    ChannelType supportedChannel();

    /**
     * Transient failures (timeouts, throttling from provider) should return a failed
     * SendResult so the caller can schedule a retry; unrecoverable errors should throw.
     */
    SendResult send(String recipient, String renderedBody);
}
