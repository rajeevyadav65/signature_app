package com.signatureapp.service;

import com.signatureapp.dto.AuditLogDto;
import com.signatureapp.exception.ResourceNotFoundException;
import com.signatureapp.model.AuditLog;
import com.signatureapp.model.Document;
import com.signatureapp.model.User;
import com.signatureapp.repository.AuditLogRepository;
import com.signatureapp.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final DocumentRepository documentRepository;

    /**
     * Log an action asynchronously (non-blocking).
     */
    @Async
    public void logAction(Document document, User user,
                          AuditLog.AuditAction action, String details,
                          String ipAddress, String userAgent) {
        try {
            AuditLog entry = AuditLog.builder()
                    .document(document)
                    .user(user)
                    .actorName(user != null ? user.getName() : "Anonymous")
                    .actorEmail(user != null ? user.getEmail() : "N/A")
                    .action(action)
                    .details(details)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    /**
     * Log action with custom actor info (for public/anonymous signers).
     */
    @Async
    public void logAction(Document document, String actorName, String actorEmail,
                          AuditLog.AuditAction action, String details,
                          String ipAddress, String userAgent) {
        try {
            AuditLog entry = AuditLog.builder()
                    .document(document)
                    .actorName(actorName != null ? actorName : "Anonymous")
                    .actorEmail(actorEmail != null ? actorEmail : "N/A")
                    .action(action)
                    .details(details)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage());
        }
    }

    /**
     * Get all audit logs for a document.
     */
    @Transactional(readOnly = true)
    public AuditLogDto.AuditLogListResponse getAuditLogsForDocument(Long documentId) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        List<AuditLog> logs = auditLogRepository.findByDocumentOrderByTimestampDesc(document);

        List<AuditLogDto.AuditLogResponse> logResponses = logs.stream()
                .map(this::toResponse)
                .toList();

        return AuditLogDto.AuditLogListResponse.builder()
                .documentId(documentId)
                .documentTitle(document.getTitle())
                .logs(logResponses)
                .totalLogs(logResponses.size())
                .build();
    }

    private AuditLogDto.AuditLogResponse toResponse(AuditLog log) {
        return AuditLogDto.AuditLogResponse.builder()
                .id(log.getId())
                .documentId(log.getDocument().getId())
                .documentTitle(log.getDocument().getTitle())
                .actorName(log.getActorName())
                .actorEmail(log.getActorEmail())
                .action(log.getAction().name())
                .details(log.getDetails())
                .ipAddress(log.getIpAddress())
                .timestamp(log.getTimestamp())
                .build();
    }
}
