package com.assetmgmt.repository;

import com.assetmgmt.entity.ProductMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductMasterRepository extends JpaRepository<ProductMaster, Long> {
    boolean existsByProductNameIgnoreCase(String productName);
    List<ProductMaster> findByCategoryId(Long categoryId);
    Optional<ProductMaster> findByProductNameIgnoreCase(String productName);
}
