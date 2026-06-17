package com.signatureapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "signatures")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signer_id")
    private User signer;

    // Signer's name (for public/external signers)
    @Column(nullable = false)
    private String signerName;

    @Column(nullable = false)
    private String signerEmail;

    // Signature position on PDF (relative coordinates)
    @Column(nullable = false)
    private Float xCoordinate;   // X position on PDF page

    @Column(nullable = false)
    private Float yCoordinate;   // Y position on PDF page

    @Column(nullable = false)
    private Integer pageNumber;  // PDF page number (1-indexed)

    private Float width;
    private Float height;

    // Signature data (base64 image or typed name)
    @Column(columnDefinition = "TEXT")
    private String signatureData;   // base64 encoded signature image

    private String signatureType;   // "DRAWN", "TYPED", "IMAGE"

    private String signerIpAddress;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SignatureStatus status = SignatureStatus.PENDING;

    private String rejectionReason;

    private LocalDateTime signedAt;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum SignatureStatus {
        PENDING,    // Awaiting signature
        SIGNED,     // Signed
        REJECTED    // Rejected
    }
}
