package com.notifsvc.template;

import com.notifsvc.channel.ChannelType;
import com.notifsvc.tenant.Tenant;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "templates", uniqueConstraints = @UniqueConstraint(columnNames = {"tenant_id", "name", "version"}))
public class Template {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @NotBlank
    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    @Column(nullable = false)
    private int version = 1;

    /** Optional, used by EMAIL channel only. */
    private String subject;

    @NotBlank
    @Lob
    @Column(nullable = false)
    private String body;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Tenant getTenant() { return tenant; }
    public void setTenant(Tenant tenant) { this.tenant = tenant; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public ChannelType getChannelType() { return channelType; }
    public void setChannelType(ChannelType channelType) { this.channelType = channelType; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
