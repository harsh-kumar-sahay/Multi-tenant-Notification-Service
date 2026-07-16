package com.notifsvc.notification;

import com.notifsvc.channel.ChannelType;

public record ChannelCount(ChannelType channelType, long count) {
}
