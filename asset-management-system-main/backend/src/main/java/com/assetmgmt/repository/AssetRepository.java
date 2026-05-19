package com.assetmgmt.repository;

import com.assetmgmt.entity.Asset;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findByAssetTag(String assetTag);
    boolean existsByAssetTag(String assetTag);
    boolean existsByCategoryId(Long categoryId);
    List<Asset> findByParentAssetId(Long parentId);

    Page<Asset> findByStatus(Asset.AssetStatus status, Pageable pageable);
    Page<Asset> findByCategoryId(Long categoryId, Pageable pageable);

    @Query("SELECT a FROM Asset a LEFT JOIN a.category c WHERE " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:categoryId IS NULL OR c.id = :categoryId) AND " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(a.name) LIKE LOWER(:search) OR " +
           "LOWER(a.assetTag) LIKE LOWER(:search) OR " +
           "LOWER(a.serialNumber) LIKE LOWER(:search))")
    Page<Asset> findWithFilters(@Param("status") Asset.AssetStatus status,
                                 @Param("categoryId") Long categoryId,
                                 @Param("search") String search,
                                 Pageable pageable);

    @Query("SELECT a.status as status, COUNT(a) as count FROM Asset a GROUP BY a.status")
    List<Object[]> countByStatus();

    long countByStatus(Asset.AssetStatus status);

    @Query(value = "SELECT * FROM assets WHERE purchase_date + (warranty_months * interval '1 month') BETWEEN :start AND :end", nativeQuery = true)
    List<Asset> findByWarrantyExpiryBetween(@Param("start") LocalDate start, @Param("end") LocalDate end);

    long countByAssetTagStartingWith(String prefix);

    @Query("SELECT COUNT(a) > 0 FROM Asset a WHERE a.createdBy.id = :userId")
    boolean existsByCreatedById(@Param("userId") Long userId);
}
