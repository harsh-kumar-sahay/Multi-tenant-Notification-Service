package com.notifsvc.channel;

public record ChannelConfigResponse(Long id, ChannelType channelType, boolean enabled, String senderIdentity) {
    public static ChannelConfigResponse from(ChannelConfig config) {
        return new ChannelConfigResponse(config.getId(), config.getChannelType(), config.isEnabled(), config.getSenderIdentity());
    }
}
