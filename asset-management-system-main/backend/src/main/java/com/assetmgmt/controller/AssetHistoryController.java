package com.assetmgmt.controller;

import com.assetmgmt.dto.AssetHistoryDto;
import com.assetmgmt.service.AssetHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/assets/{assetId}/history")
public class AssetHistoryController {

    private final AssetHistoryService assetHistoryService;

    public AssetHistoryController(AssetHistoryService assetHistoryService) {
        this.assetHistoryService = assetHistoryService;
    }

    @GetMapping
    public ResponseEntity<List<AssetHistoryDto.TimelineEvent>> getTimeline(@PathVariable Long assetId) {
        return ResponseEntity.ok(assetHistoryService.getTimeline(assetId));
    }
}
