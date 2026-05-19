package com.assetmgmt.repository;

import com.assetmgmt.entity.Procurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProcurementRepository extends JpaRepository<Procurement, Long> {

    @Query("SELECT v.name, SUM(p.totalCost) FROM Procurement p JOIN p.vendor v GROUP BY v.name")
    List<Object[]> sumSpendByVendor();

    @Query("SELECT CONCAT(YEAR(p.orderDate), '-', MONTH(p.orderDate)) as month, SUM(p.totalCost), SUM(p.quantity) " +
           "FROM Procurement p GROUP BY YEAR(p.orderDate), MONTH(p.orderDate) ORDER BY YEAR(p.orderDate) DESC, MONTH(p.orderDate) DESC")
    List<Object[]> getMonthlySpendTrends();

    @Query("SELECT SUM(p.totalCost) FROM Procurement p")
    BigDecimal getTotalSpend();

    @Query("SELECT SUM(p.quantity) FROM Procurement p")
    Long getTotalQuantity();

    @Query("SELECT COUNT(p) > 0 FROM Procurement p WHERE p.createdBy.id = :userId")
    boolean existsByCreatedById(@org.springframework.data.repository.query.Param("userId") Long userId);
}
