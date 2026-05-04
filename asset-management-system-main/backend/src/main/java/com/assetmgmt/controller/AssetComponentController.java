package com.assetmgmt.controller;

import com.assetmgmt.dto.AssetComponentDto;
import com.assetmgmt.service.AssetComponentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/assets/{assetId}/components")
public class AssetComponentController {

    private final AssetComponentService assetComponentService;

    public AssetComponentController(AssetComponentService assetComponentService) {
        this.assetComponentService = assetComponentService;
    }

    @GetMapping
    public ResponseEntity<List<AssetComponentDto.ComponentResponse>> getComponents(@PathVariable Long assetId) {
        return ResponseEntity.ok(assetComponentService.getComponents(assetId));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AssetComponentDto.ComponentResponse>> getActiveComponents(@PathVariable Long assetId) {
        return ResponseEntity.ok(assetComponentService.getActiveComponents(assetId));
    }

    @PostMapping("/replace")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<List<AssetComponentDto.ComponentResponse>> replaceComponent(
            @PathVariable Long assetId,
            @Valid @RequestBody AssetComponentDto.ReplaceComponentRequest request) {
        return ResponseEntity.ok(assetComponentService.replaceComponent(assetId, request));
    }
}
