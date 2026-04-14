package com.aigrs.backend.service;

import com.aigrs.backend.entity.Grievance;
import com.aigrs.backend.enums.GrievanceStatus;
import com.aigrs.backend.repository.GrievanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportExportService {

    private final GrievanceRepository grievanceRepository;

    public byte[] exportCsv(UUID orgId) {
        List<Grievance> grievances = grievanceRepository.findByOrgIdAndDeletedAtIsNull(orgId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        writer.println("Tracking ID,Title,Status,Priority,Location,Created At,Resolved At");
        for (Grievance g : grievances) {
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                    g.getTrackingId(), escapeCsv(g.getTitle()), g.getStatus(), g.getPriority(),
                    escapeCsv(g.getLocation()), g.getCreatedAt(), g.getResolvedAt() != null ? g.getResolvedAt() : "");
        }
        writer.flush();
        return baos.toByteArray();
    }

    public byte[] exportExcel(UUID orgId) {
        List<Grievance> grievances = grievanceRepository.findByOrgIdAndDeletedAtIsNull(orgId, org.springframework.data.domain.Pageable.unpaged()).getContent();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Grievances");

            // Header row
            Row headerRow = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            headerStyle.setFont(font);

            String[] headers = {"Tracking ID", "Title", "Status", "Priority", "Location", "Created At", "Resolved At"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowIdx = 1;
            for (Grievance g : grievances) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(g.getTrackingId());
                row.createCell(1).setCellValue(g.getTitle());
                row.createCell(2).setCellValue(g.getStatus().name());
                row.createCell(3).setCellValue(g.getPriority().name());
                row.createCell(4).setCellValue(g.getLocation());
                row.createCell(5).setCellValue(g.getCreatedAt() != null ? g.getCreatedAt().toString() : "");
                row.createCell(6).setCellValue(g.getResolvedAt() != null ? g.getResolvedAt().toString() : "");
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel report", e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        return value.replace("\"", "\"\"");
    }
}
