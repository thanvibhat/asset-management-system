package com.assetmgmt.dto;

import com.assetmgmt.entity.AssetComponent;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AssetComponentDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ComponentResponse {
        private Long id;
        private Long assetId;
        private String assetTag;
        private String componentType;
        private String serialNumber;
        private String source;
        private LocalDate installationDate;
        private LocalDate removalDate;
        private String status;
        private String oldComponentDisposition;
        private LocalDateTime createdAt;
        private Integer warrantyMonths;

        public static ComponentResponse fromEntity(AssetComponent c) {
            return ComponentResponse.builder()
                    .id(c.getId())
                    .assetId(c.getAsset() != null ? c.getAsset().getId() : null)
                    .assetTag(c.getAsset() != null ? c.getAsset().getAssetTag() : null)
                    .componentType(c.getComponentType())
                    .serialNumber(c.getSerialNumber())
                    .source(c.getSource() != null ? c.getSource().name() : null)
                    .installationDate(c.getInstallationDate())
                    .removalDate(c.getRemovalDate())
                    .status(c.getStatus() != null ? c.getStatus().name() : null)
                    .oldComponentDisposition(c.getOldComponentDisposition() != null ? c.getOldComponentDisposition().name() : null)
                    .createdAt(c.getCreatedAt())
                    .warrantyMonths(c.getWarrantyMonths())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor
    public static class ReplaceComponentRequest {
        @NotBlank(message = "Component type is required")
        private String componentType;
        
        private String serialNumber;
        
        private Integer warrantyMonths;

        private String oldComponentDisposition;
    }
}
