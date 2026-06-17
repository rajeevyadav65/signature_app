package com.signatureapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class AuditLogDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditLogResponse {
        private Long id;
        private Long documentId;
        private String documentTitle;
        private String actorName;
        private String actorEmail;
        private String action;
        private String details;
        private String ipAddress;
        private LocalDateTime timestamp;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuditLogListResponse {
        private Long documentId;
        private String documentTitle;
        private List<AuditLogResponse> logs;
        private int totalLogs;
    }
}
