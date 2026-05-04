package com.assetmgmt.controller;

import com.assetmgmt.dto.OperationsDto;
import com.assetmgmt.service.AllocationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/allocations")
public class AllocationController {

    private final AllocationService allocationService;

    public AllocationController(AllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @GetMapping
    public ResponseEntity<Page<OperationsDto.AllocationResponse>> getAllocations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(allocationService.getAllocations(PageRequest.of(page, size, Sort.by("allocatedAt").descending())));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<OperationsDto.AllocationResponse>> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(allocationService.getAllocationsByUser(userId, PageRequest.of(page, size)));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ALLOCATION_MANAGE')")
    public ResponseEntity<OperationsDto.AllocationResponse> allocateAsset(@Valid @RequestBody OperationsDto.AllocationRequest request) {
        return ResponseEntity.ok(allocationService.allocateAsset(request));
    }

    @PutMapping("/{id}/return")
    @PreAuthorize("hasAuthority('ALLOCATION_MANAGE')")
    public ResponseEntity<OperationsDto.AllocationResponse> returnAsset(
            @PathVariable Long id,
            @RequestBody(required = false) OperationsDto.ReturnRequest request) {
        return ResponseEntity.ok(allocationService.returnAsset(id, request));
    }

    @PostMapping("/{id}/reassign")
    @PreAuthorize("hasAuthority('ALLOCATION_MANAGE')")
    public ResponseEntity<OperationsDto.AllocationResponse> reassignAsset(
            @PathVariable Long id,
            @Valid @RequestBody OperationsDto.ReassignRequest request) {
        return ResponseEntity.ok(allocationService.reassignAsset(id, request));
    }
}
