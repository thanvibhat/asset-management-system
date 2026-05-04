package com.assetmgmt.service;

import com.assetmgmt.dto.OperationsDto;
import com.assetmgmt.entity.Allocation;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.entity.User;
import com.assetmgmt.exception.BusinessException;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.AllocationRepository;
import com.assetmgmt.repository.AssetRepository;
import com.assetmgmt.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Transactional
public class AllocationService {

    private final AllocationRepository allocationRepository;
    private final AssetRepository assetRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AssetHistoryService assetHistoryService;


    public AllocationService(AllocationRepository allocationRepository,
                             AssetRepository assetRepository,
                             UserRepository userRepository,
                             NotificationService notificationService,
                             AssetHistoryService assetHistoryService) {
        this.allocationRepository = allocationRepository;
        this.assetRepository = assetRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.assetHistoryService = assetHistoryService;
    }

    @Transactional(readOnly = true)
    public Page<OperationsDto.AllocationResponse> getAllocations(Pageable pageable) {
        return allocationRepository.findAll(pageable).map(OperationsDto.AllocationResponse::fromAllocation);
    }

    @Transactional(readOnly = true)
    public Page<OperationsDto.AllocationResponse> getAllocationsByUser(Long userId, Pageable pageable) {
        return allocationRepository.findByUserId(userId, pageable).map(OperationsDto.AllocationResponse::fromAllocation);
    }

    public OperationsDto.AllocationResponse allocateAsset(OperationsDto.AllocationRequest request) {
        Asset asset = assetRepository.findById(request.getAssetId())
                .orElseThrow(() -> new ResourceNotFoundException("Asset", request.getAssetId()));
        if (asset.getStatus() != Asset.AssetStatus.AVAILABLE) {
            throw new BusinessException("Asset is not available for allocation. Current status: " + asset.getStatus());
        }
        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getUserId()));
        User currentUser = getCurrentUser();

        asset.setStatus(Asset.AssetStatus.ALLOCATED);
        assetRepository.save(asset);

        Allocation allocation = Allocation.builder()
                .asset(asset)
                .user(targetUser)
                .allocatedBy(currentUser)
                .expectedReturnDate(request.getExpectedReturnDate())
                .notes(request.getNotes())
                .status(Allocation.AllocationStatus.ACTIVE)
                .build();
        Allocation savedAllocation = allocationRepository.save(allocation);
        
        try {
            java.util.Map<String,Object> meta = new java.util.HashMap<>();
            meta.put("assignedTo", targetUser.getFullName());
            meta.put("allocatedBy", currentUser.getUsername());
            if (request.getExpectedReturnDate() != null)
                meta.put("expectedReturn", request.getExpectedReturnDate().toString());
            assetHistoryService.recordEvent(
                asset.getId(),
                com.assetmgmt.entity.AssetStatusHistory.EventType.ALLOCATED,
                "AVAILABLE", "ALLOCATED",
                "Allocated to " + targetUser.getFullName(),
                currentUser.getUsername(),
                meta);
        } catch (Exception e) {
            System.err.println("History error on allocate: " + e.getMessage());
        }

        
        notificationService.sendNotification(
            targetUser,
            "New asset allocated: " + asset.getName() + " (Tag: " + asset.getAssetTag() + ")",
            "ALLOCATION"
        );
        
        return OperationsDto.AllocationResponse.fromAllocation(savedAllocation);
    }

    public OperationsDto.AllocationResponse returnAsset(Long allocationId, OperationsDto.ReturnRequest request) {
        Allocation allocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", allocationId));
        if (allocation.getStatus() != Allocation.AllocationStatus.ACTIVE) {
            throw new BusinessException("Allocation is not active");
        }
        allocation.setStatus(Allocation.AllocationStatus.RETURNED);
        allocation.setReturnedAt(LocalDateTime.now());
        allocation.setConditionAtReturn(request != null ? request.getConditionAtReturn() : null);

        Asset asset = allocation.getAsset();
        if ("DAMAGED".equals(allocation.getConditionAtReturn())) {
            asset.setStatus(Asset.AssetStatus.DAMAGED);
        } else {
            asset.setStatus(Asset.AssetStatus.AVAILABLE);
        }
        assetRepository.save(asset);

        try {
            java.util.Map<String,Object> meta = new java.util.HashMap<>();
            if (allocation.getConditionAtReturn() != null)
                meta.put("conditionAtReturn", allocation.getConditionAtReturn());
            String toSt = "DAMAGED".equals(allocation.getConditionAtReturn())
                          ? "DAMAGED" : "AVAILABLE";
            assetHistoryService.recordEvent(
                asset.getId(),
                com.assetmgmt.entity.AssetStatusHistory.EventType.RETURNED,
                "ALLOCATED", toSt,
                "Asset returned",
                getCurrentUser().getUsername(),
                meta);
        } catch (Exception e) {
            System.err.println("History error on return: " + e.getMessage());
        }


        Allocation savedAllocation = allocationRepository.save(allocation);

        notificationService.sendNotification(
            allocation.getUser(),
            "Asset returned: " + asset.getName(),
            "ALLOCATION"
        );

        return OperationsDto.AllocationResponse.fromAllocation(savedAllocation);
    }

    public OperationsDto.AllocationResponse reassignAsset(Long allocationId, OperationsDto.ReassignRequest request) {
        Allocation oldAllocation = allocationRepository.findById(allocationId)
                .orElseThrow(() -> new ResourceNotFoundException("Allocation", allocationId));
        
        if (oldAllocation.getStatus() != Allocation.AllocationStatus.ACTIVE) {
            throw new BusinessException("Allocation is not active");
        }

        User newUser = userRepository.findById(request.getNewUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getNewUserId()));
        User currentUser = getCurrentUser();
        Asset asset = oldAllocation.getAsset();

        // Close old allocation
        oldAllocation.setStatus(Allocation.AllocationStatus.RETURNED);
        oldAllocation.setReturnedAt(LocalDateTime.now());
        allocationRepository.save(oldAllocation);

        // Create new allocation
        Allocation newAllocation = Allocation.builder()
                .asset(asset)
                .user(newUser)
                .allocatedBy(currentUser)
                .expectedReturnDate(request.getExpectedReturnDate())
                .notes(request.getNotes())
                .status(Allocation.AllocationStatus.ACTIVE)
                .build();
        Allocation savedAllocation = allocationRepository.save(newAllocation);

        // Asset is already ALLOCATED, but we save it to be sure
        asset.setStatus(Asset.AssetStatus.ALLOCATED);
        assetRepository.save(asset);

        try {
            java.util.Map<String,Object> meta = new java.util.HashMap<>();
            meta.put("reassignedTo", newUser.getFullName());
            assetHistoryService.recordEvent(
                asset.getId(),
                com.assetmgmt.entity.AssetStatusHistory.EventType.REASSIGNED,
                "ALLOCATED", "ALLOCATED",
                "Reassigned to " + newUser.getFullName(),
                currentUser.getUsername(),
                meta);
        } catch (Exception e) {
            System.err.println("History error on reassign: " + e.getMessage());
        }


        notificationService.sendNotification(
            newUser,
            "Asset reassigned to you: " + asset.getName(),
            "ALLOCATION"
        );

        return OperationsDto.AllocationResponse.fromAllocation(savedAllocation);
    }

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username).orElseThrow(() -> new BusinessException("Current user not found"));
    }
}
