package com.signatureapp.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class SignatureDto {

    // ─── Place Signature Request ──────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PlaceSignatureRequest {

        @NotNull(message = "Document ID is required")
        private Long documentId;

        @NotBlank(message = "Signer name is required")
        private String signerName;

        @NotBlank(message = "Signer email is required")
        @Email
        private String signerEmail;

        @NotNull(message = "X coordinate is required")
        private Float xCoordinate;

        @NotNull(message = "Y coordinate is required")
        private Float yCoordinate;

        @NotNull(message = "Page number is required")
        private Integer pageNumber;

        private Float width;
        private Float height;
    }

    // ─── Submit Signature (Sign Document) Request ─────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubmitSignatureRequest {

        @NotBlank(message = "Signature data is required")
        private String signatureData;   // base64 encoded image

        private String signatureType;   // DRAWN / TYPED / IMAGE

        private String signerName;      // for public signing
        private String signerEmail;     // for public signing
    }

    // ─── Reject Signature Request ─────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectSignatureRequest {
        private String rejectionReason;
    }

    // ─── Signature Response ───────────────────────────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SignatureResponse {
        private Long id;
        private Long documentId;
        private String documentTitle;
        private String signerName;
        private String signerEmail;
        private Float xCoordinate;
        private Float yCoordinate;
        private Integer pageNumber;
        private Float width;
        private Float height;
        private String status;
        private String rejectionReason;
        private LocalDateTime signedAt;
        private LocalDateTime createdAt;
    }

    // ─── Finalize (Generate Signed PDF) Request ──────────────
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FinalizeRequest {
        @NotNull
        private Long documentId;
    }
}
