package com.assetmgmt.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class AnalyticsDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MaintenanceAnalytics {
        private BigDecimal totalCost;
        private long totalRecords;
        private Map<String, BigDecimal> costByCategory;
        private Map<String, Long> countByType;
        private List<MonthlyTrend> monthlyTrends;
        private List<AssetCostItem> topCostlyAssets;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProcurementAnalytics {
        private BigDecimal totalSpend;
        private long totalQuantity;
        private Map<String, BigDecimal> spendByVendor;
        private List<MonthlyTrend> monthlySpendTrends;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MonthlyTrend {
        private String month;
        private BigDecimal value;
        private long count;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AssetCostItem {
        private Long assetId;
        private String assetTag;
        private String assetName;
        private BigDecimal totalCost;
        private long maintenanceCount;
    }
}
