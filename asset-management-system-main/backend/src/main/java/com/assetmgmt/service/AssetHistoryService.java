package com.assetmgmt.service;

import com.assetmgmt.dto.AssetHistoryDto;
import com.assetmgmt.entity.Asset;
import com.assetmgmt.entity.AssetStatusHistory;
import com.assetmgmt.entity.AssetStatusHistory.EventType;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.AssetRepository;
import com.assetmgmt.repository.AssetStatusHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class AssetHistoryService {

    private final AssetStatusHistoryRepository historyRepository;
    private final AssetRepository assetRepository;
    private final ObjectMapper objectMapper;

    public AssetHistoryService(AssetStatusHistoryRepository historyRepository,
                               AssetRepository assetRepository,
                               ObjectMapper objectMapper) {
        this.historyRepository = historyRepository;
        this.assetRepository = assetRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AssetHistoryDto.TimelineEvent> getTimeline(Long assetId) {
        if (!assetRepository.existsById(assetId)) {
            throw new ResourceNotFoundException("Asset", assetId);
        }
        return historyRepository.findByAssetIdOrderByEventDateDesc(assetId).stream()
                .map(AssetHistoryDto.TimelineEvent::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void recordEvent(Long assetId, EventType eventType,
                            String fromStatus, String toStatus,
                            String notes, String performedBy,
                            Map<String, Object> metadata) {

        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset", assetId));

        AssetStatusHistory entry = AssetStatusHistory.builder()
                .asset(asset)
                .eventType(eventType)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .notes(notes)
                .performedBy(performedBy)
                .eventDate(LocalDateTime.now())
                .metadata(metadata != null ? metadata : new HashMap<>())
                .build();

        historyRepository.save(entry);
    }

    public void recordPurchase(Asset asset, String performedBy) {
        Map<String, Object> meta = new HashMap<>();
        meta.put("purchaseCost", asset.getPurchaseCost());
        meta.put("purchaseDate", asset.getPurchaseDate() != null
                ? asset.getPurchaseDate().toString() : null);

        recordEvent(asset.getId(), EventType.PURCHASED, null,
                asset.getStatus().name(), "Asset recorded in system",
                performedBy, meta);
    }

    public void recordStatusChange(Long assetId, String from, String to,
                                   String notes, String performedBy) {
        recordEvent(assetId, EventType.STATUS_CHANGED,
                from, to, notes, performedBy, null);
    }
}
