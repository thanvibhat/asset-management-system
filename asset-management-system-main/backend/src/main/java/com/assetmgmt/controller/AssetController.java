package com.assetmgmt.controller;

import com.assetmgmt.dto.AssetDto;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.exception.BusinessException;
import com.assetmgmt.repository.AssetCategoryRepository;
import com.assetmgmt.service.AssetService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;
    private final AssetCategoryRepository categoryRepository;
    private final com.assetmgmt.repository.AssetRepository assetRepository;
    private final com.assetmgmt.service.AssetExcelService excelService;

    public AssetController(AssetService assetService, 
                           AssetCategoryRepository categoryRepository,
                           com.assetmgmt.repository.AssetRepository assetRepository,
                           com.assetmgmt.service.AssetExcelService excelService) {
        this.assetService = assetService;
        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
        this.excelService = excelService;
    }

    @PostMapping("/upload")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Map<String, Object>> uploadAssets(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(assetService.uploadAssets(file));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Map<String, Object>> importAssets(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) throws Exception {

        int imported = 0, failed = 0;
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine(); // skip header
            if (headerLine == null) throw new BusinessException("Empty file");

            String line;
            int row = 2;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) { row++; continue; }
                String[] cols = line.split(",", -1);
                try {
                    AssetDto.AssetRequest req = new AssetDto.AssetRequest();
                    req.setAssetTag(cols.length > 0 ? cols[0].trim() : null);
                    req.setName(cols.length > 1 ? cols[1].trim() : null);
                    
                    // categoryName: find category by name
                    if (cols.length > 2 && !cols[2].trim().isEmpty()) {
                        categoryRepository.findByName(cols[2].trim()).ifPresent(c -> req.setCategoryId(c.getId()));
                    }
                    
                    req.setManufacturer(cols.length > 3 ? cols[3].trim() : null);
                    req.setModel(cols.length > 4 ? cols[4].trim() : null);
                    req.setSerialNumber(cols.length > 5 ? cols[5].trim() : null);
                    req.setLocation(cols.length > 6 ? cols[6].trim() : null);
                    
                    if (cols.length > 7 && !cols[7].trim().isEmpty()) req.setPurchaseDate(LocalDate.parse(cols[7].trim()));
                    if (cols.length > 8 && !cols[8].trim().isEmpty()) req.setPurchaseCost(new BigDecimal(cols[8].trim()));
                    if (cols.length > 9 && !cols[9].trim().isEmpty()) req.setCurrentValue(new BigDecimal(cols[9].trim()));
                    if (cols.length > 10 && !cols[10].trim().isEmpty()) req.setWarrantyMonths(Integer.parseInt(cols[10].trim()));
                    
                    req.setStatus(Asset.AssetStatus.AVAILABLE.name()); // Default status for imports
                    
                    assetService.createAsset(req); // reuse existing method
                    imported++;
                } catch (Exception e) {
                    errors.add("Row " + row + ": " + e.getMessage());
                    failed++;
                }
                row++;
            }
        }

        return ResponseEntity.ok(Map.of("imported", imported, "failed", failed, "errors", errors));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<byte[]> exportAssets() throws java.io.IOException {
        byte[] data = excelService.exportAssetsToExcel(assetRepository.findAll());
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=all_assets.xlsx")
                .body(data);
    }

    @GetMapping("/excel-template")
    public ResponseEntity<byte[]> downloadExcelTemplate() throws java.io.IOException {
        byte[] data = excelService.generateImportTemplate();
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .header("Content-Disposition", "attachment; filename=asset_import_template.xlsx")
                .body(data);
    }

    @GetMapping("/import/template")
    public ResponseEntity<byte[]> downloadImportTemplate() {
        String csv = "assetTag,name,categoryName,manufacturer,model,serialNumber,location,purchaseDate,purchaseCost,currentValue,warrantyMonths\n" +
                "LAP-0001,Dell Latitude 5420,Laptop,Dell,Latitude 5420,SN123456,Head Office,2024-01-15,75000,65000,36\n";
        return ResponseEntity.ok()
                .header("Content-Type", "text/csv")
                .header("Content-Disposition", "attachment; filename=asset_import_template.csv")
                .body(csv.getBytes());
    }

    @GetMapping
    public ResponseEntity<Page<AssetDto.AssetResponse>> getAssets(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Asset.AssetStatus assetStatus = (status != null && !status.trim().isEmpty()) ? Asset.AssetStatus.valueOf(status) : null;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        return ResponseEntity.ok(assetService.getAssets(assetStatus, categoryId, search, PageRequest.of(page, size, sort)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetDto.AssetResponse> getAsset(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getAsset(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ASSET_CREATE') or hasRole('ADMIN')")
    public ResponseEntity<AssetDto.AssetResponse> createAsset(@Valid @RequestBody AssetDto.AssetRequest request) {
        return ResponseEntity.ok(assetService.createAsset(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSET_UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<AssetDto.AssetResponse> updateAsset(@PathVariable Long id, @Valid @RequestBody AssetDto.AssetRequest request) {
        return ResponseEntity.ok(assetService.updateAsset(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ASSET_DELETE') or hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<List<AssetDto.AssetResponse>> getChildDevices(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.getChildAssets(id));
    }

    @PutMapping("/{id}/link/{parentId}")
    @PreAuthorize("hasAuthority('ASSET_UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<AssetDto.AssetResponse> linkAsset(@PathVariable Long id, @PathVariable Long parentId) {
        return ResponseEntity.ok(assetService.linkAsset(id, parentId));
    }

    @PutMapping("/{id}/unlink")
    @PreAuthorize("hasAuthority('ASSET_UPDATE') or hasRole('ADMIN')")
    public ResponseEntity<AssetDto.AssetResponse> unlinkAsset(@PathVariable Long id) {
        return ResponseEntity.ok(assetService.unlinkAsset(id));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<AssetDto.CategoryResponse>> getCategories() {
        return ResponseEntity.ok(assetService.getCategories());
    }

    @GetMapping("/categories/details")
    public ResponseEntity<List<AssetDto.CategoryDetailResponse>> getAllCategoryDetails() {
        return ResponseEntity.ok(assetService.getAllCategoryDetails());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetDto.CategoryDetailResponse> createCategory(@Valid @RequestBody AssetDto.CategoryRequest request) {
        return ResponseEntity.ok(assetService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetDto.CategoryDetailResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody AssetDto.CategoryRequest request) {
        return ResponseEntity.ok(assetService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        assetService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/categories/{id}/schema")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssetDto.CategoryDetailResponse> updateAttributeSchema(@PathVariable Long id, @Valid @RequestBody AssetDto.AttributeSchemaUpdateRequest request) {
        return ResponseEntity.ok(assetService.updateAttributeSchema(id, request));
    }
}
