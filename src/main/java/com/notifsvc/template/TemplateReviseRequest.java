package com.notifsvc.template;

import jakarta.validation.constraints.NotBlank;

public record TemplateReviseRequest(
        String subject,
        @NotBlank String body
) {
}
