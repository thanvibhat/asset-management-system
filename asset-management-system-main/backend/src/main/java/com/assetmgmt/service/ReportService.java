package com.assetmgmt.service;

import com.assetmgmt.entity.Allocation;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.entity.MaintenanceRecord;
import com.assetmgmt.repository.AllocationRepository;
import com.assetmgmt.repository.AssetRepository;
import com.assetmgmt.repository.MaintenanceRepository;
import com.assetmgmt.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ReportService {

    private final AssetRepository assetRepository;
    private final AllocationRepository allocationRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final VendorRepository vendorRepository;

    private final AssetAnalyticsService assetAnalyticsService;

    public byte[] exportAllocationReport() {
        List<Allocation> allocations = allocationRepository.findAll();
        StringBuilder csv = new StringBuilder("Asset Tag,Asset Name,Assigned To,Allocated At,Expected Return,Status,Serial Number,Category,Manufacturer,Model,Location,Purchase Date,Purchase Cost,Current Value,Warranty (Months)\n");
        for (Allocation a : allocations) {
            Asset asset = a.getAsset();
            csv.append(asset.getAssetTag()).append(",")
               .append("\"").append(asset.getName()).append("\",")
               .append("\"").append(a.getUser().getFullName()).append("\",")
               .append(a.getAllocatedAt()).append(",")
               .append(a.getExpectedReturnDate() != null ? a.getExpectedReturnDate() : "N/A").append(",")
               .append(a.getStatus()).append(",")
               .append("\"").append(asset.getSerialNumber() != null ? asset.getSerialNumber() : "").append("\",")
               .append(asset.getCategory() != null ? asset.getCategory().getName() : "N/A").append(",")
               .append("\"").append(asset.getManufacturer() != null ? asset.getManufacturer() : "").append("\",")
               .append("\"").append(asset.getModel() != null ? asset.getModel() : "").append("\",")
               .append("\"").append(asset.getLocation() != null ? asset.getLocation() : "").append("\",")
               .append(asset.getPurchaseDate() != null ? asset.getPurchaseDate() : "").append(",")
               .append(asset.getPurchaseCost() != null ? asset.getPurchaseCost() : 0).append(",")
               .append(asset.getCurrentValue() != null ? asset.getCurrentValue() : 0).append(",")
               .append(asset.getWarrantyMonths() != null ? asset.getWarrantyMonths() : 0).append("\n");
        }
        return csv.toString().getBytes();
    }

    public byte[] exportMaintenanceCostReport() {
        List<MaintenanceRecord> records = maintenanceRepository.findAll();
        StringBuilder csv = new StringBuilder("Asset Tag,Asset Name,Type,Cost,Vendor,Scheduled Date,Status,Serial Number,Category,Manufacturer,Model,Location,Purchase Date,Purchase Cost,Current Value,Warranty (Months)\n");
        for (MaintenanceRecord r : records) {
            Asset asset = r.getAsset();
            csv.append(asset.getAssetTag()).append(",")
               .append("\"").append(asset.getName()).append("\",")
               .append(r.getMaintenanceType()).append(",")
               .append(r.getCost() != null ? r.getCost() : 0).append(",")
               .append("\"").append(r.getVendor() != null ? r.getVendor().getName() : "N/A").append("\",")
               .append(r.getScheduledDate()).append(",")
               .append(r.getStatus()).append(",")
               .append("\"").append(asset.getSerialNumber() != null ? asset.getSerialNumber() : "").append("\",")
               .append(asset.getCategory() != null ? asset.getCategory().getName() : "N/A").append(",")
               .append("\"").append(asset.getManufacturer() != null ? asset.getManufacturer() : "").append("\",")
               .append("\"").append(asset.getModel() != null ? asset.getModel() : "").append("\",")
               .append("\"").append(asset.getLocation() != null ? asset.getLocation() : "").append("\",")
               .append(asset.getPurchaseDate() != null ? asset.getPurchaseDate() : "").append(",")
               .append(asset.getPurchaseCost() != null ? asset.getPurchaseCost() : 0).append(",")
               .append(asset.getCurrentValue() != null ? asset.getCurrentValue() : 0).append(",")
               .append(asset.getWarrantyMonths() != null ? asset.getWarrantyMonths() : 0).append("\n");
        }
        return csv.toString().getBytes();
    }

    public byte[] exportVendorPurchaseReport() {
        List<Asset> assets = assetRepository.findAll();
        Map<String, List<Asset>> grouped = assets.stream()
                .collect(Collectors.groupingBy(a -> 
                    (a.getProcurement() != null && a.getProcurement().getVendor() != null) 
                    ? a.getProcurement().getVendor().getName() 
                    : "Direct Purchase"));

        StringBuilder csv = new StringBuilder("Vendor Name,Asset Count,Total Cost\n");
        for (Map.Entry<String, List<Asset>> entry : grouped.entrySet()) {
            BigDecimal totalCost = entry.getValue().stream()
                    .map(a -> a.getPurchaseCost() != null ? a.getPurchaseCost() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            csv.append("\"").append(entry.getKey()).append("\",")
               .append(entry.getValue().size()).append(",")
               .append(totalCost).append("\n");
        }
        return csv.toString().getBytes();
    }

    public byte[] exportAssetAgingReport() {
        List<Asset> assets = assetRepository.findAll();
        StringBuilder csv = new StringBuilder("Asset Tag,Name,Category,Purchase Date,Age (Days),Current Value,Status,Serial Number,Manufacturer,Model,Location,Purchase Cost,Warranty (Months)\n");
        LocalDate now = LocalDate.now();
        for (Asset a : assets) {
            String age = "N/A";
            if (a.getPurchaseDate() != null) {
                age = String.valueOf(ChronoUnit.DAYS.between(a.getPurchaseDate(), now));
            }
            csv.append(a.getAssetTag()).append(",")
               .append("\"").append(a.getName()).append("\",")
               .append(a.getCategory() != null ? a.getCategory().getName() : "N/A").append(",")
               .append(a.getPurchaseDate() != null ? a.getPurchaseDate() : "N/A").append(",")
               .append(age).append(",")
               .append(a.getCurrentValue() != null ? a.getCurrentValue() : 0).append(",")
               .append(a.getStatus()).append(",")
               .append("\"").append(a.getSerialNumber() != null ? a.getSerialNumber() : "").append("\",")
               .append("\"").append(a.getManufacturer() != null ? a.getManufacturer() : "").append("\",")
               .append("\"").append(a.getModel() != null ? a.getModel() : "").append("\",")
               .append("\"").append(a.getLocation() != null ? a.getLocation() : "").append("\",")
               .append(a.getPurchaseCost() != null ? a.getPurchaseCost() : 0).append(",")
               .append(a.getWarrantyMonths() != null ? a.getWarrantyMonths() : 0).append("\n");
        }
        return csv.toString().getBytes();
    }

    public byte[] exportTopPerformersReport() {
        return exportMetricsCsv(assetAnalyticsService.getTopPerformers(50), "Top Performers");
    }

    public byte[] exportFrequentRepairsReport() {
        return exportMetricsCsv(assetAnalyticsService.getFrequentRepairAssets(50), "Frequent Repairs");
    }

    public byte[] exportPoorValueAssetsReport() {
        return exportMetricsCsv(assetAnalyticsService.getPoorValueAssets(), "Poor Value Assets");
    }

    private byte[] exportMetricsCsv(List<com.assetmgmt.dto.AssetMetricsDto> metrics, String title) {
        Map<String, Asset> assetMap = assetRepository.findAll().stream()
                .collect(Collectors.toMap(Asset::getAssetTag, a -> a, (a1, a2) -> a1));

        StringBuilder csv = new StringBuilder("Asset Tag,Name,Category,Maint. Cost,Repair Count,Maint. Ratio,Retention %,Age (Days),Status,Serial Number,Manufacturer,Model,Location,Purchase Date,Purchase Cost,Current Value,Warranty (Months)\n");
        for (com.assetmgmt.dto.AssetMetricsDto m : metrics) {
            Asset asset = assetMap.get(m.getAssetTag());
            csv.append(m.getAssetTag()).append(",")
               .append("\"").append(m.getAssetName()).append("\",")
               .append(m.getCategory()).append(",")
               .append(m.getTotalMaintenanceCost()).append(",")
               .append(m.getCorrectiveRepairCount()).append(",")
               .append(m.getMaintenanceToPurchaseRatio()).append(",")
               .append(m.getCurrentValueRetentionPct()).append(",")
               .append(m.getAgeInDays()).append(",");

            if (asset != null) {
                csv.append(asset.getStatus()).append(",")
                   .append("\"").append(asset.getSerialNumber() != null ? asset.getSerialNumber() : "").append("\",")
                   .append("\"").append(asset.getManufacturer() != null ? asset.getManufacturer() : "").append("\",")
                   .append("\"").append(asset.getModel() != null ? asset.getModel() : "").append("\",")
                   .append("\"").append(asset.getLocation() != null ? asset.getLocation() : "").append("\",")
                   .append(asset.getPurchaseDate() != null ? asset.getPurchaseDate() : "").append(",")
                   .append(asset.getPurchaseCost() != null ? asset.getPurchaseCost() : 0).append(",")
                   .append(asset.getCurrentValue() != null ? asset.getCurrentValue() : 0).append(",")
                   .append(asset.getWarrantyMonths() != null ? asset.getWarrantyMonths() : 0).append("\n");
            } else {
                csv.append("N/A,\"\",\"\",\"\",\"\",,0,0,0\n");
            }
        }
        return csv.toString().getBytes();
    }

    public List<com.assetmgmt.dto.DepreciationReportItemDto> getDepreciationReport(
            LocalDate targetDate, Long categoryId, String location) {
        LocalDate queryDate = targetDate != null ? targetDate : LocalDate.now();
        List<Asset> assets = assetRepository.findAll();
        
        return assets.stream()
                .filter(a -> a.getPurchaseDate() != null && !a.getPurchaseDate().isAfter(queryDate))
                .filter(a -> a.getStatus() != Asset.AssetStatus.DISPOSED || (a.getDisposalDate() != null && a.getDisposalDate().isAfter(queryDate)))
                .filter(a -> categoryId == null || (a.getCategory() != null && a.getCategory().getId().equals(categoryId)))
                .filter(a -> location == null || location.trim().isEmpty() || 
                             (a.getLocation() != null && a.getLocation().toLowerCase().contains(location.trim().toLowerCase())))
                .map(a -> {
                    BigDecimal rate = a.getDepreciationRate() != null ? a.getDepreciationRate() : BigDecimal.ZERO;
                    BigDecimal depreciatedVal = calculateDepreciatedValue(a.getPurchaseCost(), a.getPurchaseDate(), rate, queryDate);
                    long age = ChronoUnit.DAYS.between(a.getPurchaseDate(), queryDate);
                    return com.assetmgmt.dto.DepreciationReportItemDto.builder()
                            .id(a.getId())
                            .assetTag(a.getAssetTag())
                            .name(a.getName())
                            .categoryName(a.getCategory() != null ? a.getCategory().getName() : "Unassigned")
                            .location(a.getLocation())
                            .purchaseDate(a.getPurchaseDate())
                            .purchaseCost(a.getPurchaseCost() != null ? a.getPurchaseCost() : BigDecimal.ZERO)
                            .depreciationRate(rate)
                            .depreciatedValue(depreciatedVal)
                            .ageInDays(age)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public List<com.assetmgmt.dto.DisposalReportItemDto> getDisposalReport(LocalDate fromDate, LocalDate toDate) {
        List<Asset> assets = assetRepository.findAll();
        
        return assets.stream()
                .filter(a -> a.getStatus() == Asset.AssetStatus.DISPOSED)
                .filter(a -> a.getDisposalDate() != null)
                .filter(a -> fromDate == null || !a.getDisposalDate().isBefore(fromDate))
                .filter(a -> toDate == null || !a.getDisposalDate().isAfter(toDate))
                .map(a -> {
                    BigDecimal rate = a.getDepreciationRate() != null ? a.getDepreciationRate() : BigDecimal.ZERO;
                    BigDecimal depreciatedVal = calculateDepreciatedValue(a.getPurchaseCost(), a.getPurchaseDate(), rate, a.getDisposalDate());
                    return com.assetmgmt.dto.DisposalReportItemDto.builder()
                            .id(a.getId())
                            .assetTag(a.getAssetTag())
                            .name(a.getName())
                            .categoryName(a.getCategory() != null ? a.getCategory().getName() : "Unassigned")
                            .location(a.getLocation())
                            .purchaseDate(a.getPurchaseDate())
                            .purchaseCost(a.getPurchaseCost() != null ? a.getPurchaseCost() : BigDecimal.ZERO)
                            .disposalDate(a.getDisposalDate())
                            .depreciationRate(rate)
                            .depreciatedValueAtDisposal(depreciatedVal)
                            .build();
                })
                .collect(Collectors.toList());
    }

    public BigDecimal calculateDepreciatedValue(BigDecimal purchaseCost, LocalDate purchaseDate, BigDecimal rate, LocalDate targetDate) {
        if (purchaseCost == null) return BigDecimal.ZERO;
        if (purchaseDate == null || targetDate == null || targetDate.isBefore(purchaseDate)) return purchaseCost;
        if (rate == null || rate.compareTo(BigDecimal.ZERO) <= 0) return purchaseCost;
        
        long days = ChronoUnit.DAYS.between(purchaseDate, targetDate);
        if (days <= 0) return purchaseCost;
        
        BigDecimal dailyRate = rate.divide(BigDecimal.valueOf(100.0 * 365.0), 10, java.math.RoundingMode.HALF_UP);
        BigDecimal depreciationAmt = purchaseCost.multiply(dailyRate).multiply(BigDecimal.valueOf(days));
        BigDecimal value = purchaseCost.subtract(depreciationAmt);
        
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return value.setScale(2, java.math.RoundingMode.HALF_UP);
    }
}
