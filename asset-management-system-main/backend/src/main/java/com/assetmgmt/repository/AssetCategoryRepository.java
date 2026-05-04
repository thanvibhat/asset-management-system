package com.assetmgmt.repository;

import com.assetmgmt.entity.AssetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AssetCategoryRepository extends JpaRepository<AssetCategory, Long> {
    Optional<AssetCategory> findByName(String name);
    boolean existsByName(String name);
}
