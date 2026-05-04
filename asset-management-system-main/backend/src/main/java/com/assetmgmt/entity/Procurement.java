package com.assetmgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "procurement")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Procurement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", unique = true, nullable = false, length = 100)
    private String poNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_id")
    private Vendor vendor;

    @Column(name = "order_date")
    private LocalDate orderDate;

    @Column(name = "total_cost", precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(length = 50)
    private String status;

    @Column(name = "invoice_number", length = 100)
    private String invoiceNumber;

    @Column
    private Integer quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
