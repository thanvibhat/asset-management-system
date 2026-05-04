package com.assetmgmt.service;

import com.assetmgmt.dto.AssetMetricsDto;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AnalyticsContextBuilder {

    public String buildSystemPrompt(List<AssetMetricsDto> metrics) {
        // Sort and cap to top 50 by maintenance cost
        List<AssetMetricsDto> topAssets = metrics.stream()
                .sorted(Comparator.comparing(AssetMetricsDto::getTotalMaintenanceCost).reversed())
                .limit(50)
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        sb.append("You are a senior asset management analyst. Answer only using the data provided below.\n");
        sb.append("Cite specific asset names and numbers. Keep answers under 200 words unless detail is requested.\n");
        sb.append("=== ASSET PERFORMANCE DATA ===\n");

        for (AssetMetricsDto m : topAssets) {
            sb.append(String.format("- Asset: %s (%s) | Category: %s | Maint. Cost: %s | Ratio: %s | Repairs: %d | Preventive: %d | Retention: %s%% | Age: %d days\n",
                    m.getAssetName(), m.getAssetTag(), m.getCategory(),
                    m.getTotalMaintenanceCost(), m.getMaintenanceToPurchaseRatio(),
                    m.getCorrectiveRepairCount(), m.getPreventiveCount(),
                    m.getCurrentValueRetentionPct(), m.getAgeInDays()));
        }

        return sb.toString();
    }
}
