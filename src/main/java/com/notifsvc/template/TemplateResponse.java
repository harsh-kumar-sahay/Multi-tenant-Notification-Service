package com.notifsvc.template;

import com.notifsvc.channel.ChannelType;

import java.time.Instant;

public record TemplateResponse(
        Long id,
        String name,
        ChannelType channelType,
        int version,
        String subject,
        String body,
        boolean active,
        Instant createdAt
) {
    public static TemplateResponse from(Template template) {
        return new TemplateResponse(
                template.getId(),
                template.getName(),
                template.getChannelType(),
                template.getVersion(),
                template.getSubject(),
                template.getBody(),
                template.isActive(),
                template.getCreatedAt());
    }
}
