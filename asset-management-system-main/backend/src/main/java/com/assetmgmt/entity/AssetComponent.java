package com.assetmgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "asset_components")
@EntityListeners(AuditLoggerListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @Column(name = "component_type", length = 100, nullable = false)
    private String componentType;

    @Column(name = "serial_number", length = 100)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComponentSource source;

    @Column(name = "installation_date", nullable = false)
    private LocalDate installationDate;

    @Column(name = "removal_date")
    private LocalDate removalDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ComponentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "old_component_disposition", length = 30)
    private OldComponentDisposition oldComponentDisposition;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "warranty_months")
    private Integer warrantyMonths;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ComponentSource {
        OEM, REPLACED, BACKFILLED
    }

    public enum ComponentStatus {
        ACTIVE, REPLACED, FAULTY
    }

    public enum OldComponentDisposition {
        DISCARDED,
        UNDER_MAINTENANCE,
        RETURNED_TO_VENDOR,
        REPURPOSED_INTERNAL,
        IN_STORAGE,
        DONATED
    }
}
