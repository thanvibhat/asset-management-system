package com.assetmgmt.repository;

import com.assetmgmt.entity.LocationMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LocationMasterRepository extends JpaRepository<LocationMaster, Long> {
    List<LocationMaster> findByNameContainingIgnoreCase(String query);
    boolean existsByNameIgnoreCase(String name);
}
