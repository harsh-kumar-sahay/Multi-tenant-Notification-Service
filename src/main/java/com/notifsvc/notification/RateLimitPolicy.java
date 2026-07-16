package com.notifsvc.notification;

import com.notifsvc.channel.ChannelType;
import com.notifsvc.tenant.Tenant;
import jakarta.persistence.*;

@Entity
@Table(name = "rate_limit_policies", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "channel_type"}))
public class RateLimitPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    @Column(name = "max_per_minute", nullable = false)
    private int maxPerMinute = 60;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public ChannelType getChannelType() { return channelType; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }
    public int getMaxPerMinute() { return maxPerMinute; }
    public void setMaxPerMinute(int maxPerMinute) { this.maxPerMinute = maxPerMinute; }
}
