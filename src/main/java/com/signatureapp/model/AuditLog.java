package com.signatureapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Actor info (even for unauthenticated users)
    private String actorName;
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    @Column(columnDefinition = "TEXT")
    private String details;

    private String ipAddress;
    private String userAgent;

    @CreationTimestamp
    @Column(updatable = false, nullable = false)
    private LocalDateTime timestamp;

    public enum AuditAction {
        DOCUMENT_UPLOADED,
        DOCUMENT_VIEWED,
        DOCUMENT_DOWNLOADED,
        SIGNATURE_PLACED,
        SIGNATURE_SIGNED,
        SIGNATURE_REJECTED,
        SIGNED_PDF_GENERATED,
        SIGNING_LINK_GENERATED,
        SIGNING_LINK_ACCESSED,
        DOCUMENT_DELETED,
        USER_REGISTERED,
        USER_LOGIN
    }
}
