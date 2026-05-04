package com.assetmgmt.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "allocations")
@EntityListeners(AuditLoggerListener.class)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Allocation {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_by", nullable = false)
    private User allocatedBy;

    @Column(name = "allocated_at")
    private LocalDateTime allocatedAt;

    @Column(name = "returned_at")
    private LocalDateTime returnedAt;

    @Column(name = "expected_return_date")
    private LocalDate expectedReturnDate;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllocationStatus status = AllocationStatus.ACTIVE;

    @Column(name = "condition_at_return", length = 50)
    private String conditionAtReturn;

    @PrePersist
    protected void onCreate() { allocatedAt = LocalDateTime.now(); }

    public enum AllocationStatus { ACTIVE, RETURNED, OVERDUE }
}
