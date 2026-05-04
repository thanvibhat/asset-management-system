package com.assetmgmt.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.Map;

public class DashboardDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class StatsResponse {
        private long totalAssets;
        private long allocatedAssets;
        private long availableAssets;
        private long maintenanceAssets;
        private BigDecimal totalValue;
        private Map<String, Long> categoryDistribution;
        private Map<String, Long> statusDistribution;
    }

    @Getter @Setter @Builder
    public static class AllocationReportItem {
        private String assetTag;
        private String assetName;
        private String categoryName;
        private String assignedTo;
        private String allocatedAt;
        private String expectedReturnDate;
        private String status;
    }

    @Getter @Setter @Builder
    public static class MaintenanceCostReportItem {
        private String assetTag;
        private String assetName;
        private String maintenanceType;
        private BigDecimal cost;
        private String vendor;
        private String scheduledDate;
        private String status;
    }

    @Getter @Setter @Builder
    public static class VendorPurchaseReportItem {
        private String vendorName;
        private long assetCount;
        private BigDecimal totalCost;
    }
}
