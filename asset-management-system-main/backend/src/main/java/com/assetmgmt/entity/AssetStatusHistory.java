package com.assetmgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "asset_status_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", length = 50, nullable = false)
    private EventType eventType;

    @Column(name = "from_status", length = 30)
    private String fromStatus;

    @Column(name = "to_status", length = 30)
    private String toStatus;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "performed_by", length = 100)
    private String performedBy;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private java.util.Map<String, Object> metadata;

    @PrePersist
    protected void onCreate() {
        if (eventDate == null) {
            eventDate = LocalDateTime.now();
        }
        if (metadata == null) {
            metadata = new java.util.HashMap<>();
        }
    }

    public enum EventType {
        PURCHASED, ALLOCATED, RETURNED, REASSIGNED,
        MAINTENANCE_STARTED, MAINTENANCE_COMPLETED,
        STATUS_CHANGED, COMPONENT_REPLACED, WARRANTY_UPDATED, RETIRED,
        TRANSFERRED
    }
}
