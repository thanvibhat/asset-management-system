package com.assetmgmt.dto;

import com.assetmgmt.entity.Asset;
import com.assetmgmt.entity.AssetCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AssetDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AssetResponse {
        private Long id;
        private String assetTag;
        private String name;
        private String description;
        private Long categoryId;
        private String categoryName;
        private String status;
        private LocalDate purchaseDate;
        private BigDecimal purchaseCost;
        private BigDecimal currentValue;
        private String location;
        private String serialNumber;
        private String manufacturer;
        private String model;
        private Integer warrantyMonths;
        private Long productId;
        private String productName;
        private String dynamicAttributes;
        private Long vendorId;
        private String vendorName;
        private LocalDateTime createdAt;
        private Long parentId;
        private String parentTag;
        private BigDecimal depreciationRate;
        private LocalDate disposalDate;
        
        // Current Allocation Info
        private Long currentAllocationId;
        private String assignedToFullName;
        private String assignedToUsername;
        private LocalDateTime allocatedAt;
        private LocalDate expectedReturnDate;
        private LocalDate warrantyExpiryDate;

        public static AssetResponse fromAsset(Asset a) {
            return AssetResponse.builder()
                    .id(a.getId())
                    .assetTag(a.getAssetTag())
                    .name(a.getName())
                    .description(a.getDescription())
                    .categoryId(a.getCategory() != null ? a.getCategory().getId() : null)
                    .categoryName(a.getCategory() != null ? a.getCategory().getName() : null)
                    .status(a.getStatus().name())
                    .purchaseDate(a.getPurchaseDate())
                    .purchaseCost(a.getPurchaseCost())
                    .currentValue(a.getCurrentValue())
                    .location(a.getLocation())
                    .serialNumber(a.getSerialNumber())
                    .manufacturer(a.getManufacturer())
                    .model(a.getModel())
                    .warrantyMonths(a.getWarrantyMonths())
                    .productId(a.getProductMaster() != null ? a.getProductMaster().getId() : null)
                    .productName(a.getProductMaster() != null ? a.getProductMaster().getProductName() : null)
                    .dynamicAttributes(a.getDynamicAttributes())
                    .vendorId(a.getVendor() != null ? a.getVendor().getId() : null)
                    .vendorName(a.getVendor() != null ? a.getVendor().getName() : null)
                    .createdAt(a.getCreatedAt())
                    .warrantyExpiryDate(a.getWarrantyExpiryDate())
                    .parentId(a.getParentAsset() != null ? a.getParentAsset().getId() : null)
                    .parentTag(a.getParentAsset() != null ? a.getParentAsset().getAssetTag() : null)
                    .depreciationRate(a.getDepreciationRate())
                    .disposalDate(a.getDisposalDate())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AssetRequest {
        @NotBlank(message = "Asset tag is required")
        private String assetTag;

        @NotBlank(message = "Asset name is required")
        private String name;

        private String description;
        @NotNull(message = "Category is required")
        private Long categoryId;

        @NotBlank(message = "Status is required")
        private String status;
        @NotNull(message = "Purchase date is required")
        private LocalDate purchaseDate;
        private BigDecimal purchaseCost;
        private BigDecimal currentValue;
        @NotBlank(message = "Location is required")
        private String location;
        private String serialNumber;
        private String manufacturer;
        private String model;
        private Integer warrantyMonths;
        private Long productId;
        @NotNull(message = "Vendor is required")
        private Long vendorId;
        private String dynamicAttributes;
        private Long parentId;
        private BigDecimal depreciationRate;
        private LocalDate disposalDate;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CategoryResponse {
        private Long id;
        private String name;
        private String description;

        public static CategoryResponse fromCategory(AssetCategory c) {
            return CategoryResponse.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .description(c.getDescription())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CategoryRequest {
        @NotBlank(message = "Category name is required")
        private String name;
        private String description;
        private String attributeSchema;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CategoryDetailResponse {
        private Long id;
        private String name;
        private String description;
        private String attributeSchema;

        public static CategoryDetailResponse fromCategory(AssetCategory c) {
            return CategoryDetailResponse.builder()
                    .id(c.getId())
                    .name(c.getName())
                    .description(c.getDescription())
                    .attributeSchema(c.getAttributeSchema())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AttributeSchemaUpdateRequest {
        @NotNull(message = "Attribute schema is required")
        private String attributeSchema;
    }
}
