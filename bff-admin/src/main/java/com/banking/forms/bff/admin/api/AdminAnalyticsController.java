package com.banking.forms.bff.admin.api;

import com.banking.forms.analytics.application.AnalyticsExportException;
import com.banking.forms.analytics.application.AnalyticsExportService;
import com.banking.forms.analytics.application.AnalyticsRecordView;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Admin analytics export from PII-scrubbed payloads (US-9.4). */
@RestController
@RequestMapping("/api/admin/v1/analytics")
public class AdminAnalyticsController {

    private final AnalyticsExportService exportService;

    public AdminAnalyticsController(AnalyticsExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/records")
    public List<AnalyticsRecordView> listRecords(@RequestHeader("X-Tenant-Id") UUID tenantId) {
        return exportService.listRecords(tenantId);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestHeader("X-Tenant-Id") UUID tenantId,
            @RequestParam(defaultValue = "json") String format) {
        try {
            return switch (format.toLowerCase()) {
                case "csv" -> download(exportService.exportCsv(tenantId), "analytics-export.csv", "text/csv");
                case "json" -> download(exportService.exportJson(tenantId), "analytics-export.json", "application/json");
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported format: " + format);
            };
        } catch (AnalyticsExportException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        }
    }

    private static ResponseEntity<byte[]> download(byte[] body, String filename, String contentType) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(body);
    }
}
