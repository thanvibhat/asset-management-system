package com.assetmgmt.repository;

import com.assetmgmt.entity.AssetStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AssetStatusHistoryRepository extends JpaRepository<AssetStatusHistory, Long> {

    List<AssetStatusHistory> findByAssetIdOrderByEventDateDesc(Long assetId);
}
