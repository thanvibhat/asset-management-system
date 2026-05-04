package com.assetmgmt.controller;

import com.assetmgmt.entity.Procurement;
import com.assetmgmt.service.ProcurementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/procurement")
@RequiredArgsConstructor
public class ProcurementController {

    private final ProcurementService procurementService;

    @GetMapping
    public ResponseEntity<List<Procurement>> getAllProcurements() {
        return ResponseEntity.ok(procurementService.getAllProcurements());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Procurement> getProcurementById(@PathVariable Long id) {
        return procurementService.getProcurementById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Procurement> createProcurement(@RequestBody Procurement procurement, Authentication authentication) {
        return ResponseEntity.ok(procurementService.createProcurement(procurement, authentication.getName()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Procurement> updateProcurement(@PathVariable Long id, @RequestBody Procurement procurement) {
        try {
            return ResponseEntity.ok(procurementService.updateProcurement(id, procurement));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProcurement(@PathVariable Long id) {
        procurementService.deleteProcurement(id);
        return ResponseEntity.ok().build();
    }
}
