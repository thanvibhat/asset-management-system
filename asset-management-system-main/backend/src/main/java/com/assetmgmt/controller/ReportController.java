package com.assetmgmt.controller;

import com.assetmgmt.dto.AnalyticsDto;
import com.assetmgmt.dto.AssetMetricsDto;
import com.assetmgmt.dto.DashboardDto;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.repository.AssetRepository;
import com.assetmgmt.service.AssetAnalyticsService;
import com.assetmgmt.service.AnalyticsService;
import com.assetmgmt.service.DashboardService;
import com.assetmgmt.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final DashboardService dashboardService;
    private final ReportService reportService;
    private final AnalyticsService analyticsService;
    private final AssetAnalyticsService assetAnalyticsService;
    private final AssetRepository assetRepository;

    @GetMapping("/top-performers")
    public ResponseEntity<List<AssetMetricsDto>> getTopPerformers(
            @RequestParam(defaultValue = "5") int n,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(assetAnalyticsService.getTopPerformers(n, fromDate, toDate, categoryId));
    }

    @GetMapping("/high-cost-assets")
    public ResponseEntity<List<AssetMetricsDto>> getHighCostAssets(
            @RequestParam(defaultValue = "10") int n,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(assetAnalyticsService.getHighCostAssets(n, fromDate, toDate, categoryId));
    }

    @GetMapping("/frequent-repairs")
    public ResponseEntity<List<AssetMetricsDto>> getFrequentRepairs(
            @RequestParam(defaultValue = "10") int n,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(assetAnalyticsService.getFrequentRepairAssets(n, fromDate, toDate, categoryId));
    }

    @GetMapping("/poor-value-assets")
    public ResponseEntity<List<AssetMetricsDto>> getPoorValueAssets(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long categoryId) {
        return ResponseEntity.ok(assetAnalyticsService.getPoorValueAssets(fromDate, toDate, categoryId));
    }

    @GetMapping("/analytics/maintenance")
    public ResponseEntity<AnalyticsDto.MaintenanceAnalytics> getMaintenanceAnalytics() {
        return ResponseEntity.ok(analyticsService.getMaintenanceAnalytics());
    }

    @GetMapping("/analytics/procurement")
    public ResponseEntity<AnalyticsDto.ProcurementAnalytics> getProcurementAnalytics() {
        return ResponseEntity.ok(analyticsService.getProcurementAnalytics());
    }

    @GetMapping("/dashboard-stats")
    public ResponseEntity<DashboardDto.StatsResponse> getDashboardStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }

    // Helper to build a CSV download response
    private ResponseEntity<byte[]> csvResponse(byte[] data, String filename) {
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=" + filename)
                .body(data);
    }

    @GetMapping("/export/assets")
    public ResponseEntity<byte[]> exportAssets() {
        List<Asset> assets = assetRepository.findAll();
        StringBuilder csv = new StringBuilder("ID,Asset Tag,Name,Category,Status,Purchase Date,Cost,Value,Serial Number,Manufacturer,Model,Location,Warranty (Months)\n");
        for (Asset a : assets) {
            csv.append(a.getId()).append(",")
               .append(a.getAssetTag()).append(",")
               .append("\"").append(a.getName()).append("\",")
               .append(a.getCategory() != null ? a.getCategory().getName() : "N/A").append(",")
               .append(a.getStatus()).append(",")
               .append(a.getPurchaseDate()).append(",")
               .append(a.getPurchaseCost()).append(",")
               .append(a.getCurrentValue()).append(",")
               .append("\"").append(a.getSerialNumber() != null ? a.getSerialNumber() : "").append("\",")
               .append("\"").append(a.getManufacturer() != null ? a.getManufacturer() : "").append("\",")
               .append("\"").append(a.getModel() != null ? a.getModel() : "").append("\",")
               .append("\"").append(a.getLocation() != null ? a.getLocation() : "").append("\",")
               .append(a.getWarrantyMonths() != null ? a.getWarrantyMonths() : 0).append("\n");
        }
        
        byte[] data = csv.toString().getBytes();
        return csvResponse(data, "assets_report.csv");
    }

    @GetMapping("/export/allocations")
    public ResponseEntity<byte[]> exportAllocations() {
        return csvResponse(reportService.exportAllocationReport(), "allocations_report.csv");
    }

    @GetMapping("/export/maintenance-costs")
    public ResponseEntity<byte[]> exportMaintenanceCosts() {
        return csvResponse(reportService.exportMaintenanceCostReport(), "maintenance_cost_report.csv");
    }

    @GetMapping("/export/vendor-purchases")
    public ResponseEntity<byte[]> exportVendorPurchases() {
        return csvResponse(reportService.exportVendorPurchaseReport(), "vendor_purchases_report.csv");
    }

    @GetMapping("/export/asset-aging")
    public ResponseEntity<byte[]> exportAssetAging() {
        return csvResponse(reportService.exportAssetAgingReport(), "asset_aging_report.csv");
    }

    @GetMapping("/export/top-performers")
    public ResponseEntity<byte[]> exportTopPerformers() {
        return csvResponse(reportService.exportTopPerformersReport(), "top_performers_report.csv");
    }

    @GetMapping("/export/frequent-repairs")
    public ResponseEntity<byte[]> exportFrequentRepairs() {
        return csvResponse(reportService.exportFrequentRepairsReport(), "frequent_repairs_report.csv");
    }

    @GetMapping("/export/poor-value-assets")
    public ResponseEntity<byte[]> exportPoorValueAssets() {
        return csvResponse(reportService.exportPoorValueAssetsReport(), "poor_value_report.csv");
    }
}
