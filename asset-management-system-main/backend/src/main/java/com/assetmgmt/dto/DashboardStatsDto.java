package com.assetmgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDto {
    private long totalAssets;
    private long availableAssets;
    private long allocatedAssets;
    private long maintenanceAssets;
    private long retiredAssets;
    private BigDecimal totalAssetValue;
    private long activeUsers;
}
