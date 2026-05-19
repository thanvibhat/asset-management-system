package com.assetmgmt.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepreciationReportItemDto {
    private Long id;
    private String assetTag;
    private String name;
    private String categoryName;
    private String location;
    private LocalDate purchaseDate;
    private BigDecimal purchaseCost;
    private BigDecimal depreciationRate;
    private BigDecimal depreciatedValue;
    private Long ageInDays;
}
