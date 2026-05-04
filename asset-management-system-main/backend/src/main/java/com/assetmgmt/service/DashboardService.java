package com.assetmgmt.service;

import com.assetmgmt.dto.DashboardDto;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final AssetRepository assetRepository;

    public DashboardService(AssetRepository assetRepository) {
        this.assetRepository = assetRepository;
    }

    public DashboardDto.StatsResponse getStats() {
        var assets = assetRepository.findAll();
        
        long total = assets.size();
        long allocated = assets.stream().filter(a -> a.getStatus() == Asset.AssetStatus.ALLOCATED).count();
        long available = assets.stream().filter(a -> a.getStatus() == Asset.AssetStatus.AVAILABLE).count();
        long maintenance = assets.stream().filter(a -> a.getStatus() == Asset.AssetStatus.UNDER_MAINTENANCE).count();
        
        BigDecimal totalValue = assets.stream()
                .map(this::calculateAssetValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        Map<String, Long> categoryDistribution = assets.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getCategory() != null ? a.getCategory().getName() : "Uncategorized", 
                        Collectors.counting()));
        
        Map<String, Long> statusDistribution = assets.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getStatus() != null ? a.getStatus().name() : "UNKNOWN", 
                        Collectors.counting()));
        
        return DashboardDto.StatsResponse.builder()
                .totalAssets(total)
                .allocatedAssets(allocated)
                .availableAssets(available)
                .maintenanceAssets(maintenance)
                .totalValue(totalValue)
                .categoryDistribution(categoryDistribution)
                .statusDistribution(statusDistribution)
                .build();
    }

    private BigDecimal calculateAssetValue(Asset asset) {
        if (asset.getPurchaseCost() == null) return BigDecimal.ZERO;
        if (asset.getPurchaseDate() == null || asset.getProductMaster() == null || 
            asset.getProductMaster().getDepreciationPercentage() == null || 
            asset.getProductMaster().getDepreciationPercentage() <= 0) {
            return asset.getPurchaseCost();
        }

        double cost = asset.getPurchaseCost().doubleValue();
        double rate = asset.getProductMaster().getDepreciationPercentage() / 100.0;
        
        java.time.LocalDate now = java.time.LocalDate.now();
        if (asset.getPurchaseDate().isAfter(now)) return asset.getPurchaseCost();

        long totalMonths = java.time.temporal.ChronoUnit.MONTHS.between(asset.getPurchaseDate(), now);
        if (totalMonths <= 0) return asset.getPurchaseCost();

        long fullYears = totalMonths / 12;
        int remainingMonths = (int) (totalMonths % 12);

        // Calculate value after full years: V = P * (1 - r)^n
        double valueAfterYears = cost * Math.pow(1 - rate, fullYears);

        // Calculate final value after remaining months: V_final = V_years * (1 - r * m/12)
        double finalValue = valueAfterYears * (1 - rate * (remainingMonths / 12.0));

        return BigDecimal.valueOf(Math.max(0, finalValue)).setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
