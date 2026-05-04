package com.assetmgmt.controller;

import com.assetmgmt.dto.MasterDataDto;
import com.assetmgmt.entity.LocationMaster;
import com.assetmgmt.entity.ManufacturerMaster;
import com.assetmgmt.exception.BusinessException;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.LocationMasterRepository;
import com.assetmgmt.repository.ManufacturerMasterRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/master")
public class MasterDataController {

    private final LocationMasterRepository locationRepository;
    private final ManufacturerMasterRepository manufacturerRepository;

    public MasterDataController(LocationMasterRepository locationRepository,
                                ManufacturerMasterRepository manufacturerRepository) {
        this.locationRepository = locationRepository;
        this.manufacturerRepository = manufacturerRepository;
    }

    // Locations
    @GetMapping("/locations")
    public ResponseEntity<Page<LocationMaster>> getAllLocations(Pageable pageable) {
        return ResponseEntity.ok(locationRepository.findAll(pageable));
    }

    @GetMapping("/locations/all")
    public ResponseEntity<List<LocationMaster>> getLocationsList() {
        return ResponseEntity.ok(locationRepository.findAll());
    }

    @GetMapping("/locations/search")
    public ResponseEntity<List<LocationMaster>> searchLocations(@RequestParam("q") String query) {
        return ResponseEntity.ok(locationRepository.findByNameContainingIgnoreCase(query));
    }

    @PostMapping("/locations")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<LocationMaster> createLocation(@Valid @RequestBody MasterDataDto.LocationRequest request) {
        if (locationRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Location name already exists: " + request.getName());
        }
        LocationMaster location = LocationMaster.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        return new ResponseEntity<>(locationRepository.save(location), HttpStatus.CREATED);
    }

    @PutMapping("/locations/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<LocationMaster> updateLocation(@PathVariable Long id, @Valid @RequestBody MasterDataDto.LocationRequest request) {
        LocationMaster location = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Location", id));
        
        if (!location.getName().equalsIgnoreCase(request.getName()) && locationRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Location name already exists: " + request.getName());
        }
        
        location.setName(request.getName());
        location.setDescription(request.getDescription());
        return ResponseEntity.ok(locationRepository.save(location));
    }

    @DeleteMapping("/locations/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteLocation(@PathVariable Long id) {
        if (!locationRepository.existsById(id)) {
            throw new ResourceNotFoundException("Location", id);
        }
        locationRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Manufacturers
    @GetMapping("/manufacturers")
    public ResponseEntity<Page<ManufacturerMaster>> getAllManufacturers(Pageable pageable) {
        return ResponseEntity.ok(manufacturerRepository.findAll(pageable));
    }

    @GetMapping("/manufacturers/all")
    public ResponseEntity<List<ManufacturerMaster>> getManufacturersList() {
        return ResponseEntity.ok(manufacturerRepository.findAll());
    }

    @GetMapping("/manufacturers/search")
    public ResponseEntity<List<ManufacturerMaster>> searchManufacturers(@RequestParam("q") String query) {
        return ResponseEntity.ok(manufacturerRepository.findByNameContainingIgnoreCase(query));
    }

    @PostMapping("/manufacturers")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ManufacturerMaster> createManufacturer(@Valid @RequestBody MasterDataDto.ManufacturerRequest request) {
        if (manufacturerRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Manufacturer name already exists: " + request.getName());
        }
        ManufacturerMaster manufacturer = ManufacturerMaster.builder()
                .name(request.getName())
                .website(request.getWebsite())
                .build();
        return new ResponseEntity<>(manufacturerRepository.save(manufacturer), HttpStatus.CREATED);
    }

    @PutMapping("/manufacturers/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ManufacturerMaster> updateManufacturer(@PathVariable Long id, @Valid @RequestBody MasterDataDto.ManufacturerRequest request) {
        ManufacturerMaster manufacturer = manufacturerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manufacturer", id));

        if (!manufacturer.getName().equalsIgnoreCase(request.getName()) && manufacturerRepository.existsByNameIgnoreCase(request.getName())) {
            throw new BusinessException("Manufacturer name already exists: " + request.getName());
        }

        manufacturer.setName(request.getName());
        manufacturer.setWebsite(request.getWebsite());
        return ResponseEntity.ok(manufacturerRepository.save(manufacturer));
    }

    @DeleteMapping("/manufacturers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteManufacturer(@PathVariable Long id) {
        if (!manufacturerRepository.existsById(id)) {
            throw new ResourceNotFoundException("Manufacturer", id);
        }
        manufacturerRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
