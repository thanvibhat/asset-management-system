package com.assetmgmt.service;

import com.assetmgmt.dto.AssetDto;
import com.assetmgmt.entity.Allocation;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.entity.AssetCategory;
import com.assetmgmt.entity.User;
import com.assetmgmt.exception.BusinessException;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.AllocationRepository;
import com.assetmgmt.repository.AssetCategoryRepository;
import com.assetmgmt.repository.AssetRepository;
import com.assetmgmt.repository.ProductMasterRepository;
import com.assetmgmt.repository.UserRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AssetService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AssetService.class);

    private final AssetRepository assetRepository;
    private final AssetCategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final ProductMasterRepository productMasterRepository;
    private final ProductMasterService productMasterService;
    private final AllocationRepository allocationRepository;
    private final com.assetmgmt.repository.VendorRepository vendorRepository;
    private final AssetHistoryService assetHistoryService;


    public AssetService(AssetRepository assetRepository,
                        AssetCategoryRepository categoryRepository,
                        UserRepository userRepository,
                        ProductMasterRepository productMasterRepository,
                        ProductMasterService productMasterService,
                        AllocationRepository allocationRepository,
                        com.assetmgmt.repository.VendorRepository vendorRepository,
                        AssetHistoryService assetHistoryService) {
        this.assetRepository = assetRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.productMasterRepository = productMasterRepository;
        this.productMasterService = productMasterService;
        this.allocationRepository = allocationRepository;
        this.vendorRepository = vendorRepository;
        this.assetHistoryService = assetHistoryService;
    }

    @Transactional(readOnly = true)
    public Page<AssetDto.AssetResponse> getAssets(Asset.AssetStatus status, Long categoryId, String search, Pageable pageable) {
        String searchPattern = (search != null && !search.trim().isEmpty()) ? "%" + search.trim() + "%" : null;
        return assetRepository.findWithFilters(status, categoryId, searchPattern, pageable)
                .map(this::mapToAssetResponse);
    }

    @Transactional(readOnly = true)
    public AssetDto.AssetResponse getAsset(Long id) {
        Asset asset = assetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        return mapToAssetResponse(asset);
    }

    private AssetDto.AssetResponse mapToAssetResponse(Asset a) {
        try {
            AssetDto.AssetResponse resp = AssetDto.AssetResponse.fromAsset(a);
            if (a.getStatus() == Asset.AssetStatus.ALLOCATED) {
                allocationRepository.findFirstByAssetIdAndStatusOrderByAllocatedAtDesc(a.getId(), Allocation.AllocationStatus.ACTIVE)
                        .ifPresent(al -> {
                            resp.setCurrentAllocationId(al.getId());
                            resp.setAssignedToFullName(al.getUser().getFullName());
                            resp.setAssignedToUsername(al.getUser().getUsername());
                            resp.setAllocatedAt(al.getAllocatedAt());
                            resp.setExpectedReturnDate(al.getExpectedReturnDate());
                        });
            }
            return resp;
        } catch (Exception e) {
            System.err.println("Error mapping asset " + a.getId() + ": " + e.getMessage());
            e.printStackTrace();
            return AssetDto.AssetResponse.fromAsset(a); // Fallback to basic mapping
        }
    }

    public AssetDto.AssetResponse createAsset(AssetDto.AssetRequest request) {
        if (assetRepository.existsByAssetTag(request.getAssetTag())) {
            throw new BusinessException("Asset tag already exists: " + request.getAssetTag());
        }
        AssetCategory category = request.getCategoryId() != null
                ? categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()))
                : null;

        com.assetmgmt.entity.ProductMaster productMaster = request.getProductId() != null
                ? productMasterRepository.findById(request.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()))
                : null;

        com.assetmgmt.entity.Vendor vendor = request.getVendorId() != null
                ? vendorRepository.findById(request.getVendorId()).orElseThrow(() -> new ResourceNotFoundException("Vendor", request.getVendorId()))
                : null;

        Asset parentAsset = request.getParentId() != null
                ? assetRepository.findById(request.getParentId()).orElseThrow(() -> new ResourceNotFoundException("Parent Asset", request.getParentId()))
                : null;

        User currentUser = getCurrentUser();
        Asset asset = Asset.builder()
                .assetTag(request.getAssetTag())
                .name(request.getName())
                .description(request.getDescription())
                .category(category)
                .productMaster(productMaster)
                .vendor(vendor)
                .status(request.getStatus() != null ? Asset.AssetStatus.valueOf(request.getStatus()) : Asset.AssetStatus.AVAILABLE)
                .purchaseDate(request.getPurchaseDate())
                .purchaseCost(request.getPurchaseCost())
                .currentValue(request.getCurrentValue() != null ? request.getCurrentValue() : request.getPurchaseCost())
                .location(request.getLocation())
                .serialNumber(request.getSerialNumber())
                .manufacturer(request.getManufacturer())
                .model(request.getModel())
                .warrantyMonths(request.getWarrantyMonths())
                .dynamicAttributes(request.getDynamicAttributes())
                .parentAsset(parentAsset)
                .createdBy(currentUser)
                .build();
        Asset savedAsset = assetRepository.save(asset);
        try {
            assetHistoryService.recordPurchase(savedAsset, currentUser.getUsername());
        } catch (Exception e) {
            System.err.println("History error on create: " + e.getMessage());
        }
        return AssetDto.AssetResponse.fromAsset(savedAsset);
    }

    public AssetDto.AssetResponse updateAsset(Long id, AssetDto.AssetRequest request) {
        Asset asset = assetRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        if (!asset.getAssetTag().equals(request.getAssetTag()) && assetRepository.existsByAssetTag(request.getAssetTag())) {
            throw new BusinessException("Asset tag already exists: " + request.getAssetTag());
        }
        AssetCategory category = request.getCategoryId() != null
                ? categoryRepository.findById(request.getCategoryId()).orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()))
                : null;

        com.assetmgmt.entity.ProductMaster productMaster = request.getProductId() != null
                ? productMasterRepository.findById(request.getProductId()).orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()))
                : null;

        com.assetmgmt.entity.Vendor vendor = request.getVendorId() != null
                ? vendorRepository.findById(request.getVendorId()).orElseThrow(() -> new ResourceNotFoundException("Vendor", request.getVendorId()))
                : null;

        Asset parentAsset = request.getParentId() != null
                ? assetRepository.findById(request.getParentId()).orElseThrow(() -> new ResourceNotFoundException("Parent Asset", request.getParentId()))
                : null;

        asset.setAssetTag(request.getAssetTag());
        asset.setName(request.getName());
        asset.setDescription(request.getDescription());
        asset.setCategory(category);
        asset.setProductMaster(productMaster);
        asset.setVendor(vendor);
        String oldStatus = asset.getStatus().name();
        if (request.getStatus() != null) asset.setStatus(Asset.AssetStatus.valueOf(request.getStatus()));
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setPurchaseCost(request.getPurchaseCost());
        asset.setCurrentValue(request.getCurrentValue() != null ? request.getCurrentValue() : request.getPurchaseCost());
        asset.setLocation(request.getLocation());
        asset.setSerialNumber(request.getSerialNumber());
        asset.setManufacturer(request.getManufacturer());
        asset.setModel(request.getModel());
        asset.setWarrantyMonths(request.getWarrantyMonths());
        asset.setDynamicAttributes(request.getDynamicAttributes());
        asset.setParentAsset(parentAsset);
        Asset savedAsset = assetRepository.save(asset);
        try {
            String newStatus = savedAsset.getStatus().name();
            if (!oldStatus.equals(newStatus)) {
                assetHistoryService.recordStatusChange(savedAsset.getId(),
                    oldStatus, newStatus, "Status updated via edit",
                    getCurrentUser().getUsername());
            }
        } catch (Exception e) {
            System.err.println("History error on update: " + e.getMessage());
        }
        return AssetDto.AssetResponse.fromAsset(savedAsset);
    }

    public void deleteAsset(Long id) {
        if (!assetRepository.existsById(id)) throw new ResourceNotFoundException("Asset", id);
        assetRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<AssetDto.AssetResponse> getChildAssets(Long parentId) {
        if (!assetRepository.existsById(parentId)) throw new ResourceNotFoundException("Asset", parentId);
        return assetRepository.findByParentAssetId(parentId).stream()
                .map(this::mapToAssetResponse)
                .toList();
    }

    public AssetDto.AssetResponse linkAsset(Long assetId, Long parentId) {
        try {
            if (assetId.equals(parentId)) throw new BusinessException("An asset cannot be linked to itself");
            Asset asset = assetRepository.findById(assetId).orElseThrow(() -> new ResourceNotFoundException("Asset", assetId));
            Asset parent = assetRepository.findById(parentId).orElseThrow(() -> new ResourceNotFoundException("Parent Asset", parentId));
            
            asset.setParentAsset(parent);
            Asset savedAsset = assetRepository.save(asset);
            
            try {
                String details = String.format("Linked to parent asset: %s (%s)", parent.getName(), parent.getAssetTag());
                assetHistoryService.recordStatusChange(assetId, asset.getStatus().name(), asset.getStatus().name(), details, getCurrentUser().getUsername());
            } catch (Exception e) {
                log.warn("Failed to record history for linking: {}", e.getMessage());
            }
            
            return mapToAssetResponse(savedAsset);
        } catch (Exception e) {
            log.error("Error linking asset {} to parent {}", assetId, parentId, e);
            throw e;
        }
    }

    public AssetDto.AssetResponse unlinkAsset(Long assetId) {
        Asset asset = assetRepository.findById(assetId).orElseThrow(() -> new ResourceNotFoundException("Asset", assetId));
        Asset oldParent = asset.getParentAsset();
        asset.setParentAsset(null);
        Asset savedAsset = assetRepository.save(asset);
        
        try {
            if (oldParent != null) {
                String details = String.format("Unlinked from parent asset: %s (%s)", oldParent.getName(), oldParent.getAssetTag());
                assetHistoryService.recordStatusChange(assetId, asset.getStatus().name(), asset.getStatus().name(), details, getCurrentUser().getUsername());
            }
        } catch (Exception e) {
            log.warn("Failed to record history for unlinking: {}", e.getMessage());
        }
        
        return mapToAssetResponse(savedAsset);
    }


    @Transactional(readOnly = true)
    public List<AssetDto.CategoryResponse> getCategories() {
        return categoryRepository.findAll().stream().map(AssetDto.CategoryResponse::fromCategory).toList();
    }

    @Transactional(readOnly = true)
    public List<AssetDto.CategoryDetailResponse> getAllCategoryDetails() {
        return categoryRepository.findAll().stream().map(AssetDto.CategoryDetailResponse::fromCategory).toList();
    }

    public AssetDto.CategoryDetailResponse createCategory(AssetDto.CategoryRequest request) {
        if (categoryRepository.findByName(request.getName()).isPresent()) {
            throw new BusinessException("Category name already exists: " + request.getName());
        }
        AssetCategory category = AssetCategory.builder()
                .name(request.getName())
                .description(request.getDescription())
                .attributeSchema(request.getAttributeSchema())
                .build();
        return AssetDto.CategoryDetailResponse.fromCategory(categoryRepository.save(category));
    }

    public AssetDto.CategoryDetailResponse updateCategory(Long id, AssetDto.CategoryRequest request) {
        AssetCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        
        categoryRepository.findByName(request.getName()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BusinessException("Category name already exists: " + request.getName());
            }
        });

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        category.setAttributeSchema(request.getAttributeSchema());
        return AssetDto.CategoryDetailResponse.fromCategory(categoryRepository.save(category));
    }

    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category", id);
        }
        if (assetRepository.existsByCategoryId(id)) {
            throw new BusinessException("Cannot delete category with existing assets");
        }
        categoryRepository.deleteById(id);
    }

    public AssetDto.CategoryDetailResponse updateAttributeSchema(Long id, AssetDto.AttributeSchemaUpdateRequest request) {
        AssetCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        category.setAttributeSchema(request.getAttributeSchema());
        return AssetDto.CategoryDetailResponse.fromCategory(categoryRepository.save(category));
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new BusinessException("Current user not found"));
    }

    public Map<String, Object> uploadAssets(MultipartFile file) {
        int imported = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        User currentUser = getCurrentUser();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();

            // Skip header row
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    String name = formatter.formatCellValue(row.getCell(0)).trim();
                    String serialNumber = formatter.formatCellValue(row.getCell(1)).trim();
                    String categoryName = formatter.formatCellValue(row.getCell(2)).trim();
                    String statusStr = formatter.formatCellValue(row.getCell(3)).trim();
                    String purchaseDateStr = formatter.formatCellValue(row.getCell(4)).trim();
                    String costStr = formatter.formatCellValue(row.getCell(5)).trim();

                    if (name.isEmpty()) throw new BusinessException("Name is required");

                    AssetCategory category = null;
                    if (!categoryName.isEmpty()) {
                        category = categoryRepository.findByName(categoryName)
                                .orElseThrow(() -> new BusinessException("Category not found: " + categoryName));
                    }

                    Asset.AssetStatus status = Asset.AssetStatus.AVAILABLE;
                    if (!statusStr.isEmpty()) {
                        try {
                            status = Asset.AssetStatus.valueOf(statusStr.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new BusinessException("Invalid status: " + statusStr);
                        }
                    }

                    LocalDate purchaseDate = null;
                    if (!purchaseDateStr.isEmpty()) {
                        Cell dateCell = row.getCell(4);
                        if (DateUtil.isCellDateFormatted(dateCell)) {
                            purchaseDate = dateCell.getDateCellValue().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        } else {
                            try {
                                purchaseDate = LocalDate.parse(purchaseDateStr);
                            } catch (Exception e) {
                                throw new BusinessException("Invalid date format. Use YYYY-MM-DD or Excel date.");
                            }
                        }
                    }

                    BigDecimal cost = BigDecimal.ZERO;
                    if (!costStr.isEmpty()) {
                        try {
                            cost = new BigDecimal(costStr.replace(",", ""));
                        } catch (Exception e) {
                            throw new BusinessException("Invalid cost format");
                        }
                    }

                    // Generate a unique asset tag if possible, or use a default
                    String assetTag = "IMP-" + System.currentTimeMillis() + "-" + imported;

                    Asset asset = Asset.builder()
                            .assetTag(assetTag)
                            .name(name)
                            .serialNumber(serialNumber)
                            .category(category)
                            .status(status)
                            .purchaseDate(purchaseDate)
                            .purchaseCost(cost)
                            .createdBy(currentUser)
                            .build();

                    assetRepository.save(asset);
                    imported++;
                } catch (Exception e) {
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                    failed++;
                }
            }
        } catch (IOException e) {
            throw new BusinessException("Failed to read Excel file: " + e.getMessage());
        }

        return Map.of("imported", imported, "failed", failed, "errors", errors);
    }

    public AssetDto.AssetResponse transferAsset(Long id, String newLocation, String username) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", id));
        
        if (asset.getStatus() == Asset.AssetStatus.ALLOCATED) {
            throw new BusinessException("Cannot transfer allocated asset: " + asset.getAssetTag());
        }
        
        String oldLocation = asset.getLocation() != null ? asset.getLocation() : "Unassigned";
        asset.setLocation(newLocation);
        Asset savedAsset = assetRepository.save(asset);
        
        // Record in history for main asset
        try {
            assetHistoryService.recordEvent(
                    asset.getId(),
                    com.assetmgmt.entity.AssetStatusHistory.EventType.TRANSFERRED,
                    asset.getStatus().name(),
                    asset.getStatus().name(),
                    String.format("Asset transferred from location '%s' to '%s'", oldLocation, newLocation),
                    username,
                    null
            );
        } catch (Exception e) {
            log.warn("Failed to record history for transfer: {}", e.getMessage());
        }
        
        // Find and transfer unallocated child assets recursively or directly
        List<Asset> childAssets = assetRepository.findByParentAssetId(id);
        for (Asset child : childAssets) {
            if (child.getStatus() != Asset.AssetStatus.ALLOCATED) {
                String oldChildLoc = child.getLocation() != null ? child.getLocation() : "Unassigned";
                child.setLocation(newLocation);
                assetRepository.save(child);
                
                try {
                    assetHistoryService.recordEvent(
                            child.getId(),
                            com.assetmgmt.entity.AssetStatusHistory.EventType.TRANSFERRED,
                            child.getStatus().name(),
                            child.getStatus().name(),
                            String.format("Transferred along with parent asset %s from location '%s' to '%s'", 
                                    asset.getAssetTag(), oldChildLoc, newLocation),
                            username,
                            null
                    );
                } catch (Exception e) {
                    log.warn("Failed to record history for child asset transfer: {}", e.getMessage());
                }
            }
        }
        
        return mapToAssetResponse(savedAsset);
    }
}
