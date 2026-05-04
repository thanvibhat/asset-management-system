package com.assetmgmt.service;

import com.assetmgmt.entity.Vendor;
import com.assetmgmt.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    public List<Vendor> getAllVendors() {
        return vendorRepository.findAll();
    }

    public Page<Vendor> getAllVendors(Pageable pageable) {
        return vendorRepository.findAll(pageable);
    }

    public Optional<Vendor> getVendorById(Long id) {
        return vendorRepository.findById(id);
    }

    public Vendor createVendor(Vendor vendor) {
        return vendorRepository.save(vendor);
    }

    public Vendor updateVendor(Long id, Vendor updatedVendor) {
        return vendorRepository.findById(id).map(vendor -> {
            vendor.setName(updatedVendor.getName());
            vendor.setContactEmail(updatedVendor.getContactEmail());
            vendor.setContactPhone(updatedVendor.getContactPhone());
            vendor.setAddress(updatedVendor.getAddress());
            return vendorRepository.save(vendor);
        }).orElseThrow(() -> new RuntimeException("Vendor not found with id " + id));
    }

    public void deleteVendor(Long id) {
        vendorRepository.deleteById(id);
    }
}
