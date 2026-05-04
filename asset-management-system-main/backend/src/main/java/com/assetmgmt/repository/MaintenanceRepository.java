package com.assetmgmt.repository;

import com.assetmgmt.entity.MaintenanceRecord;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface MaintenanceRepository extends JpaRepository<MaintenanceRecord, Long> {
    Page<MaintenanceRecord> findByAssetId(Long assetId, Pageable pageable);
    Page<MaintenanceRecord> findByStatus(MaintenanceRecord.MaintenanceStatus status, Pageable pageable);
    long countByStatus(MaintenanceRecord.MaintenanceStatus status);

    List<MaintenanceRecord> findByStatusAndScheduledDateBetween(
            MaintenanceRecord.MaintenanceStatus status, LocalDate start, LocalDate end);
            
    List<MaintenanceRecord> findByStatus(MaintenanceRecord.MaintenanceStatus status);
    List<MaintenanceRecord> findByStatusIn(List<MaintenanceRecord.MaintenanceStatus> statuses);

    @Query("SELECT c.name, SUM(m.cost) FROM MaintenanceRecord m JOIN m.asset a JOIN a.category c WHERE m.status = 'COMPLETED' GROUP BY c.name")
    List<Object[]> sumCostByCategory();

    @Query("SELECT m.maintenanceType, COUNT(m) FROM MaintenanceRecord m GROUP BY m.maintenanceType")
    List<Object[]> countByType();

    @Query("SELECT CONCAT(YEAR(m.completedDate), '-', MONTH(m.completedDate)) as month, SUM(m.cost), COUNT(m) " +
           "FROM MaintenanceRecord m WHERE m.status = 'COMPLETED' AND m.completedDate IS NOT NULL " +
           "GROUP BY YEAR(m.completedDate), MONTH(m.completedDate) ORDER BY YEAR(m.completedDate) DESC, MONTH(m.completedDate) DESC")
    List<Object[]> getMonthlyTrends();

    @Query("SELECT a.id, a.assetTag, a.name, SUM(m.cost), COUNT(m) " +
           "FROM MaintenanceRecord m JOIN m.asset a " +
           "WHERE m.status = 'COMPLETED' " +
           "GROUP BY a.id, a.assetTag, a.name " +
           "ORDER BY SUM(m.cost) DESC")
    List<Object[]> getTopCostlyAssets(Pageable pageable);

    @Query("SELECT SUM(m.cost) FROM MaintenanceRecord m WHERE m.status = 'COMPLETED'")
    BigDecimal getTotalMaintenanceCost();

    @Query("SELECT a.id, SUM(m.cost) FROM MaintenanceRecord m JOIN m.asset a WHERE m.status = 'COMPLETED' GROUP BY a.id")
    List<Object[]> getMaintenanceCostPerAsset();

    @Query("SELECT a.id, COUNT(m) FROM MaintenanceRecord m JOIN m.asset a WHERE m.status = 'COMPLETED' AND m.maintenanceType = 'CORRECTIVE' GROUP BY a.id")
    List<Object[]> getCorrectiveCountPerAsset();

    @Query("SELECT a.id, COUNT(m) FROM MaintenanceRecord m JOIN m.asset a WHERE m.status = 'COMPLETED' AND m.maintenanceType = 'PREVENTIVE' GROUP BY a.id")
    List<Object[]> getPreventiveCountPerAsset();

    @Query("SELECT a.id, m.completedDate FROM MaintenanceRecord m JOIN m.asset a WHERE m.status = 'COMPLETED' AND m.maintenanceType = 'CORRECTIVE' AND m.completedDate IS NOT NULL ORDER BY a.id, m.completedDate")
    List<Object[]> getCorrectiveDatesPerAsset();
}
