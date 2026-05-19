package com.assetmgmt.service;

import com.assetmgmt.dto.AssetMetricsDto;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.repository.AssetRepository;
import com.assetmgmt.repository.MaintenanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AssetAnalyticsService {

    private final AssetRepository assetRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final GeminiService geminiService;

    public String getAiInsightForAsset(Long assetId) {
        AssetMetricsDto metrics = getAllMetrics().stream()
                .filter(m -> m.getAssetId().equals(assetId))
                .findFirst()
                .orElse(null);

        if (metrics == null) return "Asset not found.";

        String prompt = String.format(
            "Analyze the following asset metrics and provide a brief (2-3 sentences) strategic recommendation for its maintenance or replacement:\n" +
            "Asset: %s (%s), Category: %s\n" +
            "Total Maintenance Cost: %s\n" +
            "Maintenance to Purchase Ratio: %s\n" +
            "Corrective Repairs: %d, Preventive Maintenance: %d\n" +
            "Average Days Between Repairs: %s\n" +
            "Value Retention: %s%%\n" +
            "Age: %d days",
            metrics.getAssetName(), metrics.getAssetTag(), metrics.getCategory(),
            metrics.getTotalMaintenanceCost(), metrics.getMaintenanceToPurchaseRatio(),
            metrics.getCorrectiveRepairCount(), metrics.getPreventiveCount(),
            metrics.getAverageDaysBetweenRepairs() != null ? metrics.getAverageDaysBetweenRepairs() : "N/A",
            metrics.getCurrentValueRetentionPct(), metrics.getAgeInDays()
        );

        return geminiService.ask("", prompt);
    }

    public List<AssetMetricsDto> getAllMetrics() {
        return getAllMetrics(null, null, null);
    }

    public List<AssetMetricsDto> getAllMetrics(LocalDate fromDate, LocalDate toDate, Long categoryId) {
        List<Asset> assets = assetRepository.findAll();
        
        // Filter assets by category if categoryId is provided
        if (categoryId != null) {
            assets = assets.stream()
                .filter(asset -> asset.getCategory() != null && categoryId.equals(asset.getCategory().getId()))
                .collect(Collectors.toList());
        }
        
        // Filter assets by purchaseDate if fromDate or toDate is provided
        if (fromDate != null || toDate != null) {
            assets = assets.stream().filter(asset -> {
                if (asset.getPurchaseDate() == null) return false;
                boolean match = true;
                if (fromDate != null && asset.getPurchaseDate().isBefore(fromDate)) match = false;
                if (toDate != null && asset.getPurchaseDate().isAfter(toDate)) match = false;
                return match;
            }).collect(Collectors.toList());
        }

        // Fetch needed maintenance data based on date ranges
        Map<Long, BigDecimal> costMap;
        Map<Long, Long> correctiveCountMap;
        Map<Long, Long> preventiveCountMap;
        Map<Long, List<LocalDate>> correctiveDatesMap;

        if (fromDate != null && toDate != null) {
            costMap = toMap(maintenanceRepository.getMaintenanceCostPerAssetBetween(fromDate, toDate));
            correctiveCountMap = toCountMap(maintenanceRepository.getCorrectiveCountPerAssetBetween(fromDate, toDate));
            preventiveCountMap = toCountMap(maintenanceRepository.getPreventiveCountPerAssetBetween(fromDate, toDate));
            correctiveDatesMap = toDatesMap(maintenanceRepository.getCorrectiveDatesPerAssetBetween(fromDate, toDate));
        } else {
            costMap = toMap(maintenanceRepository.getMaintenanceCostPerAsset());
            correctiveCountMap = toCountMap(maintenanceRepository.getCorrectiveCountPerAsset());
            preventiveCountMap = toCountMap(maintenanceRepository.getPreventiveCountPerAsset());
            correctiveDatesMap = toDatesMap(maintenanceRepository.getCorrectiveDatesPerAsset());
        }

        LocalDate now = LocalDate.now();

        return assets.stream().map(asset -> {
            BigDecimal totalCost = costMap.getOrDefault(asset.getId(), BigDecimal.ZERO);
            long correctiveCount = correctiveCountMap.getOrDefault(asset.getId(), 0L);
            long preventiveCount = preventiveCountMap.getOrDefault(asset.getId(), 0L);
            
            BigDecimal purchaseCost = asset.getPurchaseCost() != null ? asset.getPurchaseCost() : BigDecimal.ONE; // Avoid div by zero
            BigDecimal maintenanceToPurchaseRatio = totalCost.divide(purchaseCost, 2, RoundingMode.HALF_UP);
            
            BigDecimal retentionPct = BigDecimal.ZERO;
            if (asset.getPurchaseCost() != null && asset.getCurrentValue() != null && asset.getPurchaseCost().compareTo(BigDecimal.ZERO) > 0) {
                retentionPct = asset.getCurrentValue().multiply(new BigDecimal("100"))
                        .divide(asset.getPurchaseCost(), 2, RoundingMode.HALF_UP);
            }

            long ageInDays = 0;
            if (asset.getPurchaseDate() != null) {
                ageInDays = ChronoUnit.DAYS.between(asset.getPurchaseDate(), now);
            }

            Double avgGap = calculateAverageGap(correctiveDatesMap.get(asset.getId()));

            AssetMetricsDto dto = AssetMetricsDto.builder()
                    .assetId(asset.getId())
                    .assetTag(asset.getAssetTag())
                    .assetName(asset.getName())
                    .category(asset.getCategory() != null ? asset.getCategory().getName() : "Uncategorized")
                    .totalMaintenanceCost(totalCost)
                    .purchaseCost(asset.getPurchaseCost())
                    .correctiveRepairCount(correctiveCount)
                    .preventiveCount(preventiveCount)
                    .maintenanceToPurchaseRatio(maintenanceToPurchaseRatio)
                    .averageDaysBetweenRepairs(avgGap)
                    .currentValueRetentionPct(retentionPct)
                    .ageInDays(ageInDays)
                    .compositeScore(retentionPct.doubleValue() - (maintenanceToPurchaseRatio.doubleValue() * 100))
                    .serialNumber(asset.getSerialNumber())
                    .manufacturer(asset.getManufacturer())
                    .model(asset.getModel())
                    .location(asset.getLocation())
                    .status(asset.getStatus() != null ? asset.getStatus().name() : "AVAILABLE")
                    .purchaseDate(asset.getPurchaseDate())
                    .warrantyMonths(asset.getWarrantyMonths())
                    .build();
            return dto;
        }).collect(Collectors.toList());
    }

    public List<AssetMetricsDto> getTopPerformers(int n) {
        return getTopPerformers(n, null, null, null);
    }

    public List<AssetMetricsDto> getTopPerformers(int n, LocalDate fromDate, LocalDate toDate, Long categoryId) {
        return getAllMetrics(fromDate, toDate, categoryId).stream()
                .sorted(Comparator.comparing(AssetMetricsDto::getCompositeScore).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    public List<AssetMetricsDto> getHighCostAssets(int n) {
        return getHighCostAssets(n, null, null, null);
    }

    public List<AssetMetricsDto> getHighCostAssets(int n, LocalDate fromDate, LocalDate toDate, Long categoryId) {
        return getAllMetrics(fromDate, toDate, categoryId).stream()
                .sorted(Comparator.comparing(AssetMetricsDto::getTotalMaintenanceCost).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    public List<AssetMetricsDto> getFrequentRepairAssets(int n) {
        return getFrequentRepairAssets(n, null, null, null);
    }

    public List<AssetMetricsDto> getFrequentRepairAssets(int n, LocalDate fromDate, LocalDate toDate, Long categoryId) {
        return getAllMetrics(fromDate, toDate, categoryId).stream()
                .sorted(Comparator.comparing(AssetMetricsDto::getCorrectiveRepairCount).reversed())
                .limit(n)
                .collect(Collectors.toList());
    }

    public List<AssetMetricsDto> getPoorValueAssets() {
        return getPoorValueAssets(null, null, null);
    }

    public List<AssetMetricsDto> getPoorValueAssets(LocalDate fromDate, LocalDate toDate, Long categoryId) {
        return getAllMetrics(fromDate, toDate, categoryId).stream()
                .filter(dto -> dto.getMaintenanceToPurchaseRatio().compareTo(new BigDecimal("0.5")) > 0)
                .collect(Collectors.toList());
    }

    // Helper methods for data processing
    private Map<Long, BigDecimal> toMap(List<Object[]> results) {
        Map<Long, BigDecimal> map = new HashMap<>();
        if (results != null) {
            for (Object[] r : results) {
                if (r != null && r[0] != null) {
                    map.put((Long) r[0], r[1] != null ? (BigDecimal) r[1] : BigDecimal.ZERO);
                }
            }
        }
        return map;
    }

    private Map<Long, Long> toCountMap(List<Object[]> results) {
        Map<Long, Long> map = new HashMap<>();
        if (results != null) {
            for (Object[] r : results) {
                if (r != null && r[0] != null) {
                    map.put((Long) r[0], r[1] != null ? (Long) r[1] : 0L);
                }
            }
        }
        return map;
    }

    private Map<Long, List<LocalDate>> toDatesMap(List<Object[]> results) {
        Map<Long, List<LocalDate>> map = new HashMap<>();
        if (results != null) {
            for (Object[] r : results) {
                if (r != null && r[0] != null && r[1] != null) {
                    Long id = (Long) r[0];
                    LocalDate date = (LocalDate) r[1];
                    map.computeIfAbsent(id, k -> new ArrayList<>()).add(date);
                }
            }
        }
        return map;
    }

    private Double calculateAverageGap(List<LocalDate> dates) {
        if (dates == null || dates.size() < 2) return null;
        
        long totalDays = 0;
        for (int i = 1; i < dates.size(); i++) {
            totalDays += ChronoUnit.DAYS.between(dates.get(i - 1), dates.get(i));
        }
        return (double) totalDays / (dates.size() - 1);
    }
}
