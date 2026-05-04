package com.assetmgmt.repository;

import com.assetmgmt.entity.ManufacturerMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ManufacturerMasterRepository extends JpaRepository<ManufacturerMaster, Long> {
    List<ManufacturerMaster> findByNameContainingIgnoreCase(String query);
    boolean existsByNameIgnoreCase(String name);
}
