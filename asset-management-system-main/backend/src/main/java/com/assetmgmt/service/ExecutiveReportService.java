package com.assetmgmt.service;

import com.assetmgmt.dto.AssetMetricsDto;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExecutiveReportService {

    private final AssetAnalyticsService analyticsService;
    private final AnalyticsContextBuilder contextBuilder;
    private final GeminiService geminiService;

    public byte[] generateExecutivePdfReport() {
        List<AssetMetricsDto> metrics = analyticsService.getAllMetrics();
        String context = contextBuilder.buildSystemPrompt(metrics);

        String llmPrompt = "Write a 3-paragraph executive summary of asset portfolio health.\n" +
                "Paragraph 1: Overall performance. Paragraph 2: Top concerns with specific asset names and numbers.\n" +
                "Paragraph 3: Recommendations — which to prioritise for maintenance or replacement. Be specific.";
        
        String summary = geminiService.ask(context, llmPrompt);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.BLACK);
            Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, new Color(108, 99, 255));
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
            Font tableHeaderFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);

            // Header
            Paragraph title = new Paragraph("Asset Management — Executive Report", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);
            
            Paragraph date = new Paragraph("Generated on: " + LocalDate.now(), normalFont);
            date.setAlignment(Element.ALIGN_CENTER);
            date.setSpacingAfter(20);
            document.add(date);

            // Section 1: Executive Summary
            document.add(new Paragraph("1. Executive Summary", sectionFont));
            Paragraph summaryPara = new Paragraph(summary, normalFont);
            summaryPara.setSpacingBefore(10);
            summaryPara.setSpacingAfter(20);
            summaryPara.setAlignment(Element.ALIGN_JUSTIFIED);
            document.add(summaryPara);

            // Section 2: Top 10 by Maintenance Cost
            document.add(new Paragraph("2. Top 10 by Maintenance Cost", sectionFont));
            PdfPTable table1 = createTable(new String[]{"Asset", "Category", "Purchase", "Maint. Cost", "Ratio"});
            metrics.stream()
                    .sorted(Comparator.comparing(AssetMetricsDto::getTotalMaintenanceCost).reversed())
                    .limit(10)
                    .forEach(m -> {
                        table1.addCell(m.getAssetName());
                        table1.addCell(m.getCategory());
                        table1.addCell(m.getPurchaseCost() != null ? m.getPurchaseCost().toString() : "0");
                        table1.addCell(m.getTotalMaintenanceCost().toString());
                        table1.addCell(m.getMaintenanceToPurchaseRatio().toString());
                    });
            table1.setSpacingBefore(10);
            table1.setSpacingAfter(20);
            document.add(table1);

            // Section 3: Frequent Repair Assets
            document.add(new Paragraph("3. Frequent Repair Assets", sectionFont));
            PdfPTable table2 = createTable(new String[]{"Asset", "Repair Count", "Avg Days Between Repairs"});
            metrics.stream()
                    .sorted(Comparator.comparing(AssetMetricsDto::getCorrectiveRepairCount).reversed())
                    .limit(10)
                    .forEach(m -> {
                        table2.addCell(m.getAssetName());
                        table2.addCell(String.valueOf(m.getCorrectiveRepairCount()));
                        table2.addCell(m.getAverageDaysBetweenRepairs() != null ? String.format("%.1f", m.getAverageDaysBetweenRepairs()) : "N/A");
                    });
            table2.setSpacingBefore(10);
            table2.setSpacingAfter(20);
            document.add(table2);

            // Section 4: Best Performing Assets
            document.add(new Paragraph("4. Best Performing Assets", sectionFont));
            PdfPTable table3 = createTable(new String[]{"Asset", "Value Retention %", "Maint. Ratio"});
            metrics.stream()
                    .sorted(Comparator.comparing(AssetMetricsDto::getCompositeScore).reversed())
                    .limit(10)
                    .forEach(m -> {
                        table3.addCell(m.getAssetName());
                        table3.addCell(m.getCurrentValueRetentionPct().toString() + "%");
                        table3.addCell(m.getMaintenanceToPurchaseRatio().toString());
                    });
            table3.setSpacingBefore(10);
            document.add(table3);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generating PDF", e);
        }
    }

    private PdfPTable createTable(String[] headers) {
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        Color headerColor = new Color(108, 99, 255);

        for (String header : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(5);
            table.addCell(cell);
        }
        return table;
    }
}
