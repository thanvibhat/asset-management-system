package com.assetmgmt.dto;

import com.assetmgmt.entity.ProductMaster;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDateTime;

public class ProductMasterDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ProductResponse {
        private Long id;
        private String productName;
        private String manufacturer;
        private Long categoryId;
        private String categoryName;
        private String description;
        private String assetPrefix;
        private String additionalAttributes;
        private Double depreciationPercentage;
        private LocalDateTime createdAt;

        public static ProductResponse fromProduct(ProductMaster p) {
            return ProductResponse.builder()
                .id(p.getId())
                .productName(p.getProductName())
                .manufacturer(p.getManufacturer())
                .categoryId(p.getCategory() != null ? p.getCategory().getId() : null)
                .categoryName(p.getCategory() != null ? p.getCategory().getName() : null)
                .description(p.getDescription())
                .assetPrefix(p.getAssetPrefix())
                .additionalAttributes(p.getAdditionalAttributes())
                .depreciationPercentage(p.getDepreciationPercentage())
                .createdAt(p.getCreatedAt())
                .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ProductRequest {
        @NotBlank(message = "Product name is required")
        private String productName;
        private String manufacturer;
        private Long categoryId;
        private String description;
        private String assetPrefix;   // optional; auto-computed if blank
        private String additionalAttributes; // JSON string
        private Double depreciationPercentage;
    }
}
