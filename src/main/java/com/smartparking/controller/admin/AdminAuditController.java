package com.smartparking.controller.admin;

import com.smartparking.dto.admin.AuditLogDto;
import com.smartparking.mapper.AdminMapper;
import com.smartparking.service.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin/audit")
public class AdminAuditController {

    private final AuditService auditService;
    private final AdminMapper adminMapper;

    public AdminAuditController(AuditService auditService, AdminMapper adminMapper) {
        this.auditService = auditService;
        this.adminMapper = adminMapper;
    }

    @GetMapping
    public ResponseEntity<List<AuditLogDto>> getAuditLogs(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to
    ) {
        List<AuditLogDto> list = auditService.getLogsBetween(from, to).stream()
                .map(adminMapper::toDto)
                .toList();
        return ResponseEntity.ok(list);
    }
}
