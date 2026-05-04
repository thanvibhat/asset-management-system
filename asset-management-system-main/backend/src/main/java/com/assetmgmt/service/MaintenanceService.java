package com.assetmgmt.service;

import com.assetmgmt.dto.OperationsDto;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.entity.MaintenanceRecord;
import com.assetmgmt.entity.User;
import com.assetmgmt.exception.BusinessException;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.AssetRepository;
import com.assetmgmt.repository.MaintenanceRepository;
import com.assetmgmt.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AssetHistoryService assetHistoryService;


    public MaintenanceService(MaintenanceRepository maintenanceRepository,
                              AssetRepository assetRepository,
                              UserRepository userRepository,
                              NotificationService notificationService,
                              AssetHistoryService assetHistoryService) {
        this.maintenanceRepository = maintenanceRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.assetHistoryService = assetHistoryService;
    }

    @Transactional(readOnly = true)
    public Page<OperationsDto.MaintenanceResponse> getAll(MaintenanceRecord.MaintenanceStatus status, Pageable pageable) {
        if (status != null) {
            return maintenanceRepository.findByStatus(status, pageable).map(OperationsDto.MaintenanceResponse::fromRecord);
        }
        return maintenanceRepository.findAll(pageable).map(OperationsDto.MaintenanceResponse::fromRecord);
    }

    @Transactional(readOnly = true)
    public Page<OperationsDto.MaintenanceResponse> getByAsset(Long assetId, Pageable pageable) {
        return maintenanceRepository.findByAssetId(assetId, pageable).map(OperationsDto.MaintenanceResponse::fromRecord);
    }

    public OperationsDto.MaintenanceResponse createRecord(OperationsDto.MaintenanceRequest request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset", request.getAssetId()));
        User currentUser = getCurrentUser();

        MaintenanceRecord.MaintenanceStatus status = request.getStatus() != null
                ? MaintenanceRecord.MaintenanceStatus.valueOf(request.getStatus())
                : MaintenanceRecord.MaintenanceStatus.SCHEDULED;

        if (status == MaintenanceRecord.MaintenanceStatus.IN_PROGRESS || status == MaintenanceRecord.MaintenanceStatus.SCHEDULED) {
            asset.setStatus(Asset.AssetStatus.UNDER_MAINTENANCE);
            assetRepository.save(asset);
        } else if (status == MaintenanceRecord.MaintenanceStatus.COMPLETED || status == MaintenanceRecord.MaintenanceStatus.CANCELLED) {
            if (asset.getStatus() == Asset.AssetStatus.UNDER_MAINTENANCE) {
                asset.setStatus(Asset.AssetStatus.AVAILABLE);
                assetRepository.save(asset);
            }
        }

        MaintenanceRecord record = MaintenanceRecord.builder()
                .asset(asset)
                .maintenanceType(MaintenanceRecord.MaintenanceType.valueOf(request.getMaintenanceType()))
                .description(request.getDescription())
                .cost(request.getCost())
                .performedBy(request.getPerformedBy())
                .scheduledDate(request.getScheduledDate())
                .completedDate(request.getCompletedDate())
                .status(status)
                .createdBy(currentUser)
                .build();
        MaintenanceRecord savedRecord = maintenanceRepository.save(record);

        try {
            java.util.Map<String, Object> meta = new java.util.HashMap<>();
            meta.put("maintenanceType", record.getMaintenanceType().name());
            meta.put("description", record.getDescription());
            
            com.assetmgmt.entity.AssetStatusHistory.EventType eventType = 
                (status == MaintenanceRecord.MaintenanceStatus.COMPLETED)
                ? com.assetmgmt.entity.AssetStatusHistory.EventType.MAINTENANCE_COMPLETED
                : com.assetmgmt.entity.AssetStatusHistory.EventType.MAINTENANCE_STARTED;

            assetHistoryService.recordEvent(
                asset.getId(),
                eventType,
                null, asset.getStatus().name(),
                "Maintenance " + status.name().toLowerCase(),
                currentUser.getUsername(),
                meta);
        } catch (Exception e) {
            System.err.println("History error on maintenance create: " + e.getMessage());
        }

        return OperationsDto.MaintenanceResponse.fromRecord(savedRecord);
    }

    public OperationsDto.MaintenanceResponse updateRecord(Long id, OperationsDto.MaintenanceRequest request) {
        MaintenanceRecord record = maintenanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Maintenance record", id));

        MaintenanceRecord.MaintenanceStatus newStatus = request.getStatus() != null
                ? MaintenanceRecord.MaintenanceStatus.valueOf(request.getStatus())
                : record.getStatus();

        // Handle Asset Status Transitions
        Asset asset = record.getAsset();
        if (newStatus == MaintenanceRecord.MaintenanceStatus.COMPLETED || newStatus == MaintenanceRecord.MaintenanceStatus.CANCELLED) {
            if (asset.getStatus() == Asset.AssetStatus.UNDER_MAINTENANCE) {
                asset.setStatus(Asset.AssetStatus.AVAILABLE);
                assetRepository.save(asset);
            }
            if (newStatus == MaintenanceRecord.MaintenanceStatus.COMPLETED && record.getStatus() != MaintenanceRecord.MaintenanceStatus.COMPLETED) {
                notificationService.sendNotification(
                    record.getCreatedBy(),
                    "Maintenance completed for asset: " + asset.getName(),
                    "MAINTENANCE"
                );
            }
        } else if (newStatus == MaintenanceRecord.MaintenanceStatus.SCHEDULED || newStatus == MaintenanceRecord.MaintenanceStatus.IN_PROGRESS) {
            if (asset.getStatus() != Asset.AssetStatus.UNDER_MAINTENANCE) {
                asset.setStatus(Asset.AssetStatus.UNDER_MAINTENANCE);
                assetRepository.save(asset);
            }
        }

        record.setMaintenanceType(MaintenanceRecord.MaintenanceType.valueOf(request.getMaintenanceType()));
        record.setDescription(request.getDescription());
        record.setCost(request.getCost());
        record.setPerformedBy(request.getPerformedBy());
        record.setScheduledDate(request.getScheduledDate());
        record.setCompletedDate(request.getCompletedDate());
        record.setStatus(newStatus);
        MaintenanceRecord savedRecord = maintenanceRepository.save(record);

        try {
            if (newStatus != record.getStatus()) {
                java.util.Map<String, Object> meta = new java.util.HashMap<>();
                meta.put("maintenanceType", record.getMaintenanceType().name());
                
                com.assetmgmt.entity.AssetStatusHistory.EventType eventType = null;
                if (newStatus == MaintenanceRecord.MaintenanceStatus.IN_PROGRESS) {
                    eventType = com.assetmgmt.entity.AssetStatusHistory.EventType.MAINTENANCE_STARTED;
                } else if (newStatus == MaintenanceRecord.MaintenanceStatus.COMPLETED) {
                    eventType = com.assetmgmt.entity.AssetStatusHistory.EventType.MAINTENANCE_COMPLETED;
                }

                if (eventType != null) {
                    assetHistoryService.recordEvent(
                        asset.getId(),
                        eventType,
                        null, asset.getStatus().name(),
                        "Maintenance status updated to " + newStatus.name(),
                        getCurrentUser().getUsername(),
                        meta);
                }
            }
        } catch (Exception e) {
            System.err.println("History error on maintenance update: " + e.getMessage());
        }

        return OperationsDto.MaintenanceResponse.fromRecord(savedRecord);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new BusinessException("Current user not found"));
    }
}
