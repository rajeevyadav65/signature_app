package com.signatureapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public class DocumentDto {

    // ─── Upload Response ──────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentResponse {
        private Long id;
        private String title;
        private String originalFileName;
        private Long fileSize;
        private String status;
        private String ownerName;
        private String ownerEmail;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String downloadUrl;
        private String signedDownloadUrl;
        private Integer totalSignatures;
        private Integer pendingSignatures;
        private Integer completedSignatures;
    }

    // ─── Document List Item ───────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DocumentListResponse {
        private Long id;
        private String title;
        private String originalFileName;
        private Long fileSize;
        private String status;
        private LocalDateTime createdAt;
        private Integer totalSignatures;
        private Integer pendingSignatures;
    }

    // ─── Generate Signing Link Request ───────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateSigningLinkRequest {
        private Integer expiryHours;   // default 72 hours
        private String signerEmail;    // optional — for notification
        private String signerName;     // optional
    }

    // ─── Signing Link Response ────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SigningLinkResponse {
        private String signingUrl;
        private String token;
        private LocalDateTime expiresAt;
    }

    // ─── Dashboard Stats ──────────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DashboardStats {
        private long totalDocuments;
        private long pendingDocuments;
        private long signedDocuments;
        private long rejectedDocuments;
        private List<DocumentListResponse> recentDocuments;
    }
}
