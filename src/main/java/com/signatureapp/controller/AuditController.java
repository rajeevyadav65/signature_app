package com.signatureapp.controller;

import com.signatureapp.dto.ApiResponse;
import com.signatureapp.dto.AuditLogDto;
import com.signatureapp.security.UserDetailsImpl;
import com.signatureapp.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogService auditLogService;

    /**
     * GET /api/audit/{docId}
     * Get the complete audit trail for a document.
     */
    @GetMapping("/{docId}")
    public ResponseEntity<ApiResponse<AuditLogDto.AuditLogListResponse>> getAuditLogs(
            @PathVariable Long docId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        AuditLogDto.AuditLogListResponse response =
                auditLogService.getAuditLogsForDocument(docId);
        return ResponseEntity.ok(ApiResponse.success("Audit logs retrieved", response));
    }
}
