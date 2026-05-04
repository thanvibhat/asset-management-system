package com.assetmgmt.controller;

import com.assetmgmt.dto.OperationsDto;
import com.assetmgmt.service.MaintenanceService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private final MaintenanceService maintenanceService;

    public MaintenanceController(MaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @GetMapping
    public ResponseEntity<Page<OperationsDto.MaintenanceResponse>> getAll(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        com.assetmgmt.entity.MaintenanceRecord.MaintenanceStatus statusEnum = null;
        if (status != null && !status.isEmpty()) {
            statusEnum = com.assetmgmt.entity.MaintenanceRecord.MaintenanceStatus.valueOf(status);
        }
        return ResponseEntity.ok(maintenanceService.getAll(statusEnum, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @GetMapping("/asset/{assetId}")
    public ResponseEntity<Page<OperationsDto.MaintenanceResponse>> getByAsset(
            @PathVariable Long assetId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(maintenanceService.getByAsset(assetId, PageRequest.of(page, size)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<OperationsDto.MaintenanceResponse> createRecord(@Valid @RequestBody OperationsDto.MaintenanceRequest request) {
        return ResponseEntity.ok(maintenanceService.createRecord(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<OperationsDto.MaintenanceResponse> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody OperationsDto.MaintenanceRequest request) {
        return ResponseEntity.ok(maintenanceService.updateRecord(id, request));
    }
}
