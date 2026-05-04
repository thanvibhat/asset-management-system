package com.assetmgmt.service;

import com.assetmgmt.dto.ProductMasterDto;
import com.assetmgmt.entity.AssetCategory;
import com.assetmgmt.entity.ProductMaster;
import com.assetmgmt.exception.BusinessException;
import com.assetmgmt.exception.ResourceNotFoundException;
import com.assetmgmt.repository.AssetCategoryRepository;
import com.assetmgmt.repository.AssetRepository;
import com.assetmgmt.repository.ProductMasterRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class ProductMasterService {

    private final ProductMasterRepository productMasterRepository;
    private final AssetCategoryRepository categoryRepository;
    private final AssetRepository assetRepository;

    public ProductMasterService(ProductMasterRepository productMasterRepository,
                                AssetCategoryRepository categoryRepository,
                                AssetRepository assetRepository) {
        this.productMasterRepository = productMasterRepository;
        this.categoryRepository = categoryRepository;
        this.assetRepository = assetRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductMasterDto.ProductResponse> getAllProducts() {
        return productMasterRepository.findAll().stream()
                .map(ProductMasterDto.ProductResponse::fromProduct)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductMasterDto.ProductResponse> getAllProducts(Pageable pageable) {
        return productMasterRepository.findAll(pageable)
                .map(ProductMasterDto.ProductResponse::fromProduct);
    }

    @Transactional(readOnly = true)
    public ProductMasterDto.ProductResponse getProduct(Long id) {
        ProductMaster product = productMasterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return ProductMasterDto.ProductResponse.fromProduct(product);
    }

    public ProductMasterDto.ProductResponse createProduct(ProductMasterDto.ProductRequest request) {
        if (productMasterRepository.existsByProductNameIgnoreCase(request.getProductName())) {
            throw new BusinessException("Product name already exists: " + request.getProductName());
        }

        AssetCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        }

        String assetPrefix = request.getAssetPrefix();
        if (assetPrefix == null || assetPrefix.isBlank()) {
            String name = request.getProductName();
            assetPrefix = name.length() >= 3 ? name.substring(0, 3).toUpperCase() : name.toUpperCase();
        }

        ProductMaster product = ProductMaster.builder()
                .productName(request.getProductName())
                .manufacturer(request.getManufacturer())
                .category(category)
                .description(request.getDescription())
                .assetPrefix(assetPrefix)
                .additionalAttributes(request.getAdditionalAttributes())
                .depreciationPercentage(request.getDepreciationPercentage())
                .build();

        return ProductMasterDto.ProductResponse.fromProduct(productMasterRepository.save(product));
    }

    public ProductMasterDto.ProductResponse updateProduct(Long id, ProductMasterDto.ProductRequest request) {
        ProductMaster product = productMasterRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (!product.getProductName().equalsIgnoreCase(request.getProductName()) &&
                productMasterRepository.existsByProductNameIgnoreCase(request.getProductName())) {
            throw new BusinessException("Product name already exists: " + request.getProductName());
        }

        AssetCategory category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.getCategoryId()));
        }

        product.setProductName(request.getProductName());
        product.setManufacturer(request.getManufacturer());
        product.setCategory(category);
        product.setDescription(request.getDescription());
        product.setAssetPrefix(request.getAssetPrefix());
        product.setAdditionalAttributes(request.getAdditionalAttributes());
        product.setDepreciationPercentage(request.getDepreciationPercentage());

        return ProductMasterDto.ProductResponse.fromProduct(productMasterRepository.save(product));
    }

    public void deleteProduct(Long id) {
        if (!productMasterRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", id);
        }
        productMasterRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public String getNextAssetTag(Long productId) {
        ProductMaster product = productMasterRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", productId));

        long count = assetRepository.countByAssetTagStartingWith(product.getAssetPrefix());
        return product.getAssetPrefix() + "-" + String.format("%04d", count + 1);
    }
}
