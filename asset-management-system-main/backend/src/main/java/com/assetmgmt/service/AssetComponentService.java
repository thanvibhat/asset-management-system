package com.assetmgmt.service;

import com.assetmgmt.dto.AssetComponentDto;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.entity.AssetComponent;
import com.assetmgmt.entity.AssetComponent.ComponentSource;
import com.assetmgmt.entity.AssetComponent.ComponentStatus;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.AssetComponentRepository;
import com.assetmgmt.repository.AssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssetComponentService {

    private final AssetComponentRepository componentRepository;
    private final AssetRepository assetRepository;
    private final AssetHistoryService assetHistoryService;


    public AssetComponentService(AssetComponentRepository componentRepository,
                                 AssetRepository assetRepository,
                                 AssetHistoryService assetHistoryService) {
        this.componentRepository = componentRepository;
        this.assetRepository = assetRepository;
        this.assetHistoryService = assetHistoryService;
    }

    @Transactional(readOnly = true)
    public List<AssetComponentDto.ComponentResponse> getComponents(Long assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new ResourceNotFoundException("Asset", assetId);
        }
        return componentRepository.findByAssetId(assetId).stream()
                .map(AssetComponentDto.ComponentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AssetComponentDto.ComponentResponse> getActiveComponents(Long assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new ResourceNotFoundException("Asset", assetId);
        }
        return componentRepository.findByAssetIdAndStatus(assetId, ComponentStatus.ACTIVE).stream()
                .map(AssetComponentDto.ComponentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<AssetComponentDto.ComponentResponse> replaceComponent(Long assetId, AssetComponentDto.ReplaceComponentRequest request) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", assetId));

        Optional<AssetComponent> existing = componentRepository.findByAssetIdAndComponentTypeIgnoreCaseAndStatus(
                assetId, request.getComponentType(), ComponentStatus.ACTIVE);

        if (request.getOldComponentDisposition() == null
            || request.getOldComponentDisposition().isBlank()) {
            throw new com.assetmgmt.exception.BusinessException("Please specify what happened to the old component (Disposition is required)");
        }

        if (existing.isEmpty()) {
            // First time this component type is tracked for this asset - create a BACKFILLED record
            AssetComponent backfilled = AssetComponent.builder()
                    .asset(asset)
                    .componentType(request.getComponentType())
                    .source(ComponentSource.BACKFILLED)
                    .installationDate(asset.getPurchaseDate() != null ? asset.getPurchaseDate() : LocalDate.now())
                    .removalDate(LocalDate.now())
                    .status(ComponentStatus.REPLACED)
                    .oldComponentDisposition(AssetComponent.OldComponentDisposition.valueOf(request.getOldComponentDisposition()))
                    .build();
            componentRepository.save(backfilled);
        } else {
            // Mark existing as REPLACED
            AssetComponent current = existing.get();
            current.setStatus(ComponentStatus.REPLACED);
            current.setRemovalDate(LocalDate.now());
            current.setOldComponentDisposition(
                AssetComponent.OldComponentDisposition
                    .valueOf(request.getOldComponentDisposition())
            );
            
            componentRepository.save(current);
        }

        // Create new ACTIVE record
        AssetComponent newComponent = AssetComponent.builder()
                .asset(asset)
                .componentType(request.getComponentType())
                .serialNumber(request.getSerialNumber())
                .source(ComponentSource.REPLACED)
                .installationDate(LocalDate.now())
                .status(ComponentStatus.ACTIVE)
                .warrantyMonths(request.getWarrantyMonths())
                .build();
        componentRepository.save(newComponent);

        try {
            java.util.Map<String,Object> meta = new java.util.HashMap<>();
            meta.put("componentType", request.getComponentType());
            if (request.getSerialNumber() != null)
                meta.put("newSerialNumber", request.getSerialNumber());
            assetHistoryService.recordEvent(
                assetId,
                com.assetmgmt.entity.AssetStatusHistory.EventType.COMPONENT_REPLACED,
                null, null,
                request.getComponentType() + " replaced",
                null,
                meta);
        } catch (Exception e) {
            System.err.println("History error on component replace: " + e.getMessage());
        }

        return getActiveComponents(assetId);
    }
}
