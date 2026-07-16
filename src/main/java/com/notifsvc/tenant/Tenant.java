package com.notifsvc.tenant;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "tenants")
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Column(name = "global_rate_limit_per_minute")
    private Integer globalRateLimitPerMinute;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public TenantStatus getStatus() { return status; }
    public void setStatus(TenantStatus status) { this.status = status; }
    public Integer getGlobalRateLimitPerMinute() { return globalRateLimitPerMinute; }
    public void setGlobalRateLimitPerMinute(Integer globalRateLimitPerMinute) { this.globalRateLimitPerMinute = globalRateLimitPerMinute; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
