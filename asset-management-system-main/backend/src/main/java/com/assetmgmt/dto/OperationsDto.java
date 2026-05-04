package com.assetmgmt.dto;

import com.assetmgmt.entity.Allocation;
import com.assetmgmt.entity.MaintenanceRecord;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OperationsDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AllocationResponse {
        private Long id;
        private Long assetId;
        private String assetTag;
        private String assetName;
        private Long userId;
        private String userName;
        private String userFullName;
        private String allocatedByName;
        private LocalDateTime allocatedAt;
        private LocalDateTime returnedAt;
        private LocalDate expectedReturnDate;
        private String notes;
        private String status;
        private String conditionAtReturn;

        public static AllocationResponse fromAllocation(Allocation a) {
            return AllocationResponse.builder()
                    .id(a.getId())
                    .assetId(a.getAsset().getId())
                    .assetTag(a.getAsset().getAssetTag())
                    .assetName(a.getAsset().getName())
                    .userId(a.getUser().getId())
                    .userName(a.getUser().getUsername())
                    .userFullName(a.getUser().getFullName())
                    .allocatedByName(a.getAllocatedBy().getFullName())
                    .allocatedAt(a.getAllocatedAt())
                    .returnedAt(a.getReturnedAt())
                    .expectedReturnDate(a.getExpectedReturnDate())
                    .notes(a.getNotes())
                    .status(a.getStatus().name())
                    .conditionAtReturn(a.getConditionAtReturn())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class AllocationRequest {
        @NotNull(message = "Asset ID is required")
        private Long assetId;

        @NotNull(message = "User ID is required")
        private Long userId;

        private LocalDate expectedReturnDate;

        @NotBlank(message = "Allocation details are required")
        private String notes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ReturnRequest {
        private String conditionAtReturn;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ReassignRequest {
        @NotNull(message = "New user ID is required")
        private Long newUserId;
        private LocalDate expectedReturnDate;
        private String notes;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class MaintenanceResponse {
        private Long id;
        private Long assetId;
        private String assetTag;
        private String assetName;
        private String maintenanceType;
        private String description;
        private BigDecimal cost;
        private String performedBy;
        private LocalDate scheduledDate;
        private LocalDate completedDate;
        private String status;
        private LocalDateTime createdAt;

        public static MaintenanceResponse fromRecord(MaintenanceRecord r) {
            return MaintenanceResponse.builder()
                    .id(r.getId())
                    .assetId(r.getAsset().getId())
                    .assetTag(r.getAsset().getAssetTag())
                    .assetName(r.getAsset().getName())
                    .maintenanceType(r.getMaintenanceType().name())
                    .description(r.getDescription())
                    .cost(r.getCost())
                    .performedBy(r.getPerformedBy())
                    .scheduledDate(r.getScheduledDate())
                    .completedDate(r.getCompletedDate())
                    .status(r.getStatus().name())
                    .createdAt(r.getCreatedAt())
                    .build();
        }
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class MaintenanceRequest {
        @NotNull(message = "Asset ID is required")
        private Long assetId;

        @NotBlank(message = "Maintenance type is required")
        private String maintenanceType;

        @NotBlank(message = "Description is required")
        private String description;

        private BigDecimal cost;
        private String performedBy;
        private LocalDate scheduledDate;
        private LocalDate completedDate;
        private String status;
    }
}
