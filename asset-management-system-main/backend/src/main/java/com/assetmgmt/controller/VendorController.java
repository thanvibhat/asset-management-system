package com.assetmgmt.controller;

import com.assetmgmt.entity.Vendor;
import com.assetmgmt.service.VendorService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@RequiredArgsConstructor
public class VendorController {

    private final VendorService vendorService;

    @GetMapping
    public ResponseEntity<Page<Vendor>> getAllVendors(Pageable pageable) {
        return ResponseEntity.ok(vendorService.getAllVendors(pageable));
    }

    @GetMapping("/all")
    public ResponseEntity<List<Vendor>> getVendorsList() {
        return ResponseEntity.ok(vendorService.getAllVendors());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Vendor> getVendorById(@PathVariable Long id) {
        return vendorService.getVendorById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Vendor> createVendor(@RequestBody Vendor vendor) {
        return ResponseEntity.ok(vendorService.createVendor(vendor));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Vendor> updateVendor(@PathVariable Long id, @RequestBody Vendor vendor) {
        try {
            return ResponseEntity.ok(vendorService.updateVendor(id, vendor));
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long id) {
        vendorService.deleteVendor(id);
        return ResponseEntity.ok().build();
    }
}
