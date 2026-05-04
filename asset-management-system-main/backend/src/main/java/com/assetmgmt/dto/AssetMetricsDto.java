package com.assetmgmt.dto;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AssetMetricsDto {
    private Long assetId;
    private String assetTag;
    private String assetName;
    private String category;

    private BigDecimal totalMaintenanceCost;
    private BigDecimal purchaseCost;
    private long correctiveRepairCount;
    private long preventiveCount;
    private BigDecimal maintenanceToPurchaseRatio;
    private Double averageDaysBetweenRepairs;
    private BigDecimal currentValueRetentionPct;
    private long ageInDays;

    // Added for composite score in sorting
    private Double compositeScore;
}
