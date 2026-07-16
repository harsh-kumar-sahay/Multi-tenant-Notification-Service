package com.notifsvc.template;

import com.notifsvc.channel.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record TemplateCreateRequest(
        @NotBlank String name,
        @NotNull ChannelType channelType,
        String subject,
        @NotBlank String body
) {
}
