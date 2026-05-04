package com.assetmgmt.repository;

import com.assetmgmt.entity.AssetComponent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssetComponentRepository extends JpaRepository<AssetComponent, Long> {

    List<AssetComponent> findByAssetId(Long assetId);

    List<AssetComponent> findByAssetIdAndStatus(
            Long assetId, AssetComponent.ComponentStatus status);

    Optional<AssetComponent> findByAssetIdAndComponentTypeIgnoreCaseAndStatus(
            Long assetId, String componentType, AssetComponent.ComponentStatus status);
}
