package com.assetmgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "product_master")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductMaster {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private AssetCategory category;

    @Column(length = 100)
    private String manufacturer;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Asset tag prefix, e.g. "LAP" for laptops. Auto-populated with first 3 chars of productName if not set.
    @Column(name = "asset_prefix", length = 10)
    private String assetPrefix;

    // JSON array of attribute definitions: [{"name":"RAM","dataType":"String","mandatory":true}, ...]
    // Valid dataType values: "String", "Number", "Boolean", "Date"
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "additional_attributes", columnDefinition = "jsonb")
    private String additionalAttributes;

    @Column(name = "depreciation_percentage")
    private Double depreciationPercentage;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        // Auto-set prefix from first 3 chars of productName if not explicitly provided
        if (assetPrefix == null || assetPrefix.isBlank()) {
            assetPrefix = productName != null && productName.length() >= 3
                ? productName.substring(0, 3).toUpperCase()
                : (productName != null ? productName.toUpperCase() : "AST");
        }
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
