package com.assetmgmt.dto;

import com.assetmgmt.entity.AssetStatusHistory;
import lombok.*;
import java.time.LocalDateTime;

public class AssetHistoryDto {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TimelineEvent {
        private Long id;
        private String eventType;
        private String fromStatus;
        private String toStatus;
        private String notes;
        private String performedBy;
        private LocalDateTime eventDate;
        private java.util.Map<String, Object> metadata;
        private String label;

        public static TimelineEvent fromEntity(AssetStatusHistory h) {
            return TimelineEvent.builder()
                .id(h.getId())
                .eventType(h.getEventType().name())
                .fromStatus(h.getFromStatus())
                .toStatus(h.getToStatus())
                .notes(h.getNotes())
                .performedBy(h.getPerformedBy())
                .eventDate(h.getEventDate())
                .metadata(h.getMetadata())
                .label(buildLabel(h))
                .build();
        }

        private static String buildLabel(AssetStatusHistory h) {
            return switch (h.getEventType()) {
                case PURCHASED            -> "Asset purchased";
                case ALLOCATED            -> "Asset allocated";
                case RETURNED             -> "Asset returned";
                case REASSIGNED           -> "Asset reassigned";
                case MAINTENANCE_STARTED  -> "Sent to maintenance";
                case MAINTENANCE_COMPLETED-> "Maintenance completed";
                case STATUS_CHANGED       -> "Status changed: "
                                             + h.getFromStatus() + " → " + h.getToStatus();
                case COMPONENT_REPLACED   -> "Component replaced";
                case WARRANTY_UPDATED     -> "Warranty updated";
                case RETIRED              -> "Asset retired";
            };
        }
    }
}
