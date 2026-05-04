package com.assetmgmt.service;

import com.assetmgmt.entity.Asset;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Service
public class AssetExcelService {

    public byte[] exportAssetsToExcel(List<Asset> assets) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Assets");

            // Header Row
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Asset Tag", "Name", "Serial Number", "Category", "Status", "Location", "Manufacturer", "Model", "Purchase Date", "Purchase Cost", "Warranty (Months)", "Warranty Expiry", "Description"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowIdx = 1;
            for (Asset asset : assets) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(asset.getAssetTag());
                row.createCell(1).setCellValue(asset.getName());
                row.createCell(2).setCellValue(asset.getSerialNumber());
                row.createCell(3).setCellValue(asset.getCategory() != null ? asset.getCategory().getName() : "");
                row.createCell(4).setCellValue(asset.getStatus().name());
                row.createCell(5).setCellValue(asset.getLocation());
                row.createCell(6).setCellValue(asset.getManufacturer());
                row.createCell(7).setCellValue(asset.getModel());
                row.createCell(8).setCellValue(asset.getPurchaseDate() != null ? asset.getPurchaseDate().toString() : "");
                row.createCell(9).setCellValue(asset.getPurchaseCost() != null ? asset.getPurchaseCost().doubleValue() : 0.0);
                row.createCell(10).setCellValue(asset.getWarrantyMonths() != null ? asset.getWarrantyMonths() : 0);
                row.createCell(11).setCellValue(asset.getWarrantyExpiryDate() != null ? asset.getWarrantyExpiryDate().toString() : "");
                row.createCell(12).setCellValue(asset.getDescription());
            }

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    public byte[] generateImportTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Template");

            // Header Row (matching AssetService.uploadAssets columns)
            Row headerRow = sheet.createRow(0);
            String[] columns = {"Name", "Serial Number", "Category", "Status", "Purchase Date", "Cost"};
            
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);

            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            // Sample Data
            Row sampleRow = sheet.createRow(1);
            sampleRow.createCell(0).setCellValue("Dell Latitude 5420");
            sampleRow.createCell(1).setCellValue("SN-123456");
            sampleRow.createCell(2).setCellValue("Laptop");
            sampleRow.createCell(3).setCellValue("AVAILABLE");
            sampleRow.createCell(4).setCellValue("2024-01-15");
            sampleRow.createCell(5).setCellValue(75000.0);

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
