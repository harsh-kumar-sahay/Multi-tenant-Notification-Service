package com.notifsvc.channel;

import com.notifsvc.tenant.Tenant;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "channel_configs", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "channel_type"}))
public class ChannelConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    @Column(nullable = false)
    private boolean enabled = true;

    /** Sender identity shown to recipients, e.g. "from" address / sender id. Not a real provider credential. */
    private String senderIdentity;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public ChannelType getChannelType() { return channelType; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSenderIdentity() { return senderIdentity; }
    public void setSenderIdentity(String senderIdentity) { this.senderIdentity = senderIdentity; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
