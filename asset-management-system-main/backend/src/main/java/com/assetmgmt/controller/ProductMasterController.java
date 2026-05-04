package com.assetmgmt.controller;

import com.assetmgmt.dto.ProductMasterDto;
import com.assetmgmt.service.ProductMasterService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductMasterController {

    private final ProductMasterService productMasterService;

    public ProductMasterController(ProductMasterService productMasterService) {
        this.productMasterService = productMasterService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<Page<ProductMasterDto.ProductResponse>> getAllProducts(Pageable pageable) {
        return ResponseEntity.ok(productMasterService.getAllProducts(pageable));
    }

    @GetMapping("/all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductMasterDto.ProductResponse>> getProductsList() {
        return ResponseEntity.ok(productMasterService.getAllProducts());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductMasterDto.ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productMasterService.getProduct(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductMasterDto.ProductResponse> createProduct(@Valid @RequestBody ProductMasterDto.ProductRequest request) {
        return new ResponseEntity<>(productMasterService.createProduct(request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductMasterDto.ProductResponse> updateProduct(@PathVariable Long id, @Valid @RequestBody ProductMasterDto.ProductRequest request) {
        return ResponseEntity.ok(productMasterService.updateProduct(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        productMasterService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/next-tag")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> getNextAssetTag(@PathVariable Long id) {
        return ResponseEntity.ok(productMasterService.getNextAssetTag(id));
    }
}
