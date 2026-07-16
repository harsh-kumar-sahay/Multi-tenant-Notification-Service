package com.notifsvc.auth;

public record LoginResponse(String token, String role, Long tenantId) {
}
