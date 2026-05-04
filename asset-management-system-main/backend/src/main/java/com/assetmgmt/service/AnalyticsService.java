package com.assetmgmt.service;

import com.assetmgmt.dto.AnalyticsDto;
import com.assetmgmt.repository.MaintenanceRepository;
import com.assetmgmt.repository.ProcurementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AnalyticsService {

    private final MaintenanceRepository maintenanceRepository;
    private final ProcurementRepository procurementRepository;

    public AnalyticsDto.MaintenanceAnalytics getMaintenanceAnalytics() {
        BigDecimal totalCost = maintenanceRepository.getTotalMaintenanceCost();
        if (totalCost == null) totalCost = BigDecimal.ZERO;

        Map<String, BigDecimal> costByCategory = maintenanceRepository.sumCostByCategory().stream()
                .collect(Collectors.toMap(
                        o -> (String) o[0],
                        o -> (BigDecimal) o[1]
                ));

        Map<String, Long> countByType = maintenanceRepository.countByType().stream()
                .collect(Collectors.toMap(
                        o -> o[0].toString(),
                        o -> (Long) o[1]
                ));

        List<AnalyticsDto.MonthlyTrend> monthlyTrends = maintenanceRepository.getMonthlyTrends().stream()
                .map(o -> AnalyticsDto.MonthlyTrend.builder()
                        .month((String) o[0])
                        .value((BigDecimal) o[1])
                        .count((Long) o[2])
                        .build())
                .collect(Collectors.toList());

        List<AnalyticsDto.AssetCostItem> topCostlyAssets = maintenanceRepository.getTopCostlyAssets(PageRequest.of(0, 5)).stream()
                .map(o -> AnalyticsDto.AssetCostItem.builder()
                        .assetId((Long) o[0])
                        .assetTag((String) o[1])
                        .assetName((String) o[2])
                        .totalCost((BigDecimal) o[3])
                        .maintenanceCount((Long) o[4])
                        .build())
                .collect(Collectors.toList());

        return AnalyticsDto.MaintenanceAnalytics.builder()
                .totalCost(totalCost)
                .totalRecords(maintenanceRepository.count())
                .costByCategory(costByCategory)
                .countByType(countByType)
                .monthlyTrends(monthlyTrends)
                .topCostlyAssets(topCostlyAssets)
                .build();
    }

    public AnalyticsDto.ProcurementAnalytics getProcurementAnalytics() {
        BigDecimal totalSpend = procurementRepository.getTotalSpend();
        if (totalSpend == null) totalSpend = BigDecimal.ZERO;

        Long totalQty = procurementRepository.getTotalQuantity();
        if (totalQty == null) totalQty = 0L;

        Map<String, BigDecimal> spendByVendor = procurementRepository.sumSpendByVendor().stream()
                .collect(Collectors.toMap(
                        o -> (String) o[0],
                        o -> (BigDecimal) o[1]
                ));

        List<AnalyticsDto.MonthlyTrend> monthlySpendTrends = procurementRepository.getMonthlySpendTrends().stream()
                .map(o -> AnalyticsDto.MonthlyTrend.builder()
                        .month((String) o[0])
                        .value((BigDecimal) o[1])
                        .count((Long) o[2])
                        .build())
                .collect(Collectors.toList());

        return AnalyticsDto.ProcurementAnalytics.builder()
                .totalSpend(totalSpend)
                .totalQuantity(totalQty)
                .spendByVendor(spendByVendor)
                .monthlySpendTrends(monthlySpendTrends)
                .build();
    }
}
