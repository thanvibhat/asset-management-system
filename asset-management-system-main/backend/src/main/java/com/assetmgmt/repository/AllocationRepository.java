package com.assetmgmt.repository;

import com.assetmgmt.entity.Allocation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface AllocationRepository extends JpaRepository<Allocation, Long> {
    Page<Allocation> findByUserId(Long userId, Pageable pageable);
    Page<Allocation> findByAssetId(Long assetId, Pageable pageable);
    Page<Allocation> findByStatus(Allocation.AllocationStatus status, Pageable pageable);
    List<Allocation> findByStatus(Allocation.AllocationStatus status);
    Optional<Allocation> findFirstByAssetIdAndStatusOrderByAllocatedAtDesc(Long assetId, Allocation.AllocationStatus status);
    boolean existsByAssetIdAndStatus(Long assetId, Allocation.AllocationStatus status);
    long countByStatus(Allocation.AllocationStatus status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) > 0 FROM Allocation a WHERE a.user.id = :userId")
    boolean existsByUserId(@org.springframework.data.repository.query.Param("userId") Long userId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(a) > 0 FROM Allocation a WHERE a.allocatedBy.id = :allocatedById")
    boolean existsByAllocatedById(@org.springframework.data.repository.query.Param("allocatedById") Long allocatedById);
}
