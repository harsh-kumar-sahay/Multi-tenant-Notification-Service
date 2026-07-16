package com.notifsvc.channel;

import jakarta.validation.constraints.NotNull;

public record ChannelConfigRequest(
        @NotNull ChannelType channelType,
        boolean enabled,
        String senderIdentity
) {
}
