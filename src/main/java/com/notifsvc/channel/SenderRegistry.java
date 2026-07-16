package com.notifsvc.channel;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class SenderRegistry {

    private final Map<ChannelType, NotificationSender> sendersByChannel;

    public SenderRegistry(List<NotificationSender> senders) {
        this.sendersByChannel = senders.stream()
                .collect(Collectors.toMap(NotificationSender::supportedChannel, Function.identity()));
    }

    public NotificationSender get(ChannelType channelType) {
        NotificationSender sender = sendersByChannel.get(channelType);
        if (sender == null) {
            throw new IllegalStateException("No NotificationSender registered for channel " + channelType);
        }
        return sender;
    }
}
