package com.signatureapp.service;

import com.signatureapp.dto.DocumentDto;
import com.signatureapp.exception.BadRequestException;
import com.signatureapp.exception.ResourceNotFoundException;
import com.signatureapp.exception.UnauthorizedException;
import com.signatureapp.model.AuditLog;
import com.signatureapp.model.Document;
import com.signatureapp.model.User;
import com.signatureapp.repository.DocumentRepository;
import com.signatureapp.repository.SignatureRepository;
import com.signatureapp.repository.UserRepository;
import com.signatureapp.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final SignatureRepository signatureRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ─── Upload Document ──────────────────────────────────────
    @Transactional
    public DocumentDto.DocumentResponse uploadDocument(MultipartFile file, String title,
                                                       UserDetailsImpl currentUser,
                                                       HttpServletRequest request) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        User owner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        String storedFileName = fileStorageService.storeFile(file);
        String filePath       = fileStorageService.getFilePath(storedFileName);
        String docTitle       = (title != null && !title.isBlank())
                ? title : file.getOriginalFilename();

        Document document = Document.builder()
                .title(docTitle)
                .originalFileName(file.getOriginalFilename())
                .storedFileName(storedFileName)
                .filePath(filePath)
                .fileSize(file.getSize())
                .status(Document.DocumentStatus.PENDING)
                .owner(owner)
                .build();

        document = documentRepository.save(document);
        log.info("Document uploaded: {} by user: {}", docTitle, owner.getEmail());

        auditLogService.logAction(document, owner, AuditLog.AuditAction.DOCUMENT_UPLOADED,
                "Document uploaded: " + docTitle,
                getClientIp(request), request.getHeader("User-Agent"));

        return toDocumentResponse(document);
    }

    // ─── List Documents ───────────────────────────────────────
    @Transactional(readOnly = true)
    public List<DocumentDto.DocumentListResponse> getMyDocuments(UserDetailsImpl currentUser,
                                                                  String status) {
        User owner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        List<Document> docs;
        if (status != null && !status.isBlank()) {
            Document.DocumentStatus docStatus = parseStatus(status);
            docs = documentRepository.findByOwnerAndStatusOrderByCreatedAtDesc(owner, docStatus);
        } else {
            docs = documentRepository.findByOwnerOrderByCreatedAtDesc(owner);
        }

        return docs.stream().map(this::toListResponse).toList();
    }

    // ─── Get Document by ID ───────────────────────────────────
    @Transactional(readOnly = true)
    public DocumentDto.DocumentResponse getDocumentById(Long id, UserDetailsImpl currentUser,
                                                         HttpServletRequest request) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));

        User requester = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not own this document");
        }

        auditLogService.logAction(document, requester, AuditLog.AuditAction.DOCUMENT_VIEWED,
                "Document viewed", getClientIp(request), request.getHeader("User-Agent"));

        return toDocumentResponse(document);
    }

    // ─── Generate Signing Link ────────────────────────────────
    @Transactional
    public DocumentDto.SigningLinkResponse generateSigningLink(
            Long documentId,
            DocumentDto.GenerateSigningLinkRequest request,
            UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not own this document");
        }

        String token   = UUID.randomUUID().toString().replace("-", "");
        int expiryHrs  = (request != null && request.getExpiryHours() != null)
                ? request.getExpiryHours() : 72;
        LocalDateTime expiry = LocalDateTime.now().plusHours(expiryHrs);

        document.setSigningToken(token);
        document.setSigningTokenExpiry(expiry);
        documentRepository.save(document);

        User owner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        auditLogService.logAction(document, owner, AuditLog.AuditAction.SIGNING_LINK_GENERATED,
                "Signing link generated (expires: " + expiry + ")",
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

        // Send email notification if signerEmail provided
        if (request != null && request.getSignerEmail() != null && !request.getSignerEmail().isBlank()) {
            emailService.sendSigningRequestEmail(
                    request.getSignerEmail(),
                    request.getSignerName() != null ? request.getSignerName() : "Signer",
                    document.getTitle(),
                    owner.getName(),
                    token);
        }

        String signingUrl = baseUrl + "/api/public/sign/" + token;
        return DocumentDto.SigningLinkResponse.builder()
                .signingUrl(signingUrl)
                .token(token)
                .expiresAt(expiry)
                .build();
    }

    // ─── Get Document by Signing Token (Public) ───────────────
    @Transactional(readOnly = true)
    public DocumentDto.DocumentResponse getDocumentByToken(String token,
                                                            HttpServletRequest request) {
        Document document = documentRepository.findBySigningToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or expired signing link"));

        if (document.getSigningTokenExpiry() != null &&
                document.getSigningTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This signing link has expired");
        }

        auditLogService.logAction(document, "Anonymous", "N/A",
                AuditLog.AuditAction.SIGNING_LINK_ACCESSED,
                "Signing link accessed", getClientIp(request), request.getHeader("User-Agent"));

        return toDocumentResponse(document);
    }

    // ─── Dashboard Stats ──────────────────────────────────────
    @Transactional(readOnly = true)
    public DocumentDto.DashboardStats getDashboardStats(UserDetailsImpl currentUser) {
        User owner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        long total    = documentRepository.countByOwner(owner);
        long pending  = documentRepository.countByOwnerAndStatus(owner, Document.DocumentStatus.PENDING);
        long signed   = documentRepository.countByOwnerAndStatus(owner, Document.DocumentStatus.SIGNED);
        long rejected = documentRepository.countByOwnerAndStatus(owner, Document.DocumentStatus.REJECTED);

        List<DocumentDto.DocumentListResponse> recent =
                documentRepository.findByOwnerOrderByCreatedAtDesc(owner)
                        .stream().limit(5).map(this::toListResponse).toList();

        return DocumentDto.DashboardStats.builder()
                .totalDocuments(total)
                .pendingDocuments(pending)
                .signedDocuments(signed)
                .rejectedDocuments(rejected)
                .recentDocuments(recent)
                .build();
    }

    // ─── Delete Document ──────────────────────────────────────
    @Transactional
    public void deleteDocument(Long documentId, UserDetailsImpl currentUser) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not own this document");
        }

        fileStorageService.deleteFile(document.getStoredFileName());
        if (document.getSignedFilePath() != null) {
            fileStorageService.deleteFile("signed/" + fileStorageService.generateSignedFileName(
                    document.getStoredFileName()));
        }

        documentRepository.delete(document);
        log.info("Document deleted: {} by user: {}", document.getTitle(), currentUser.getEmail());
    }

    // ─── Mappers ──────────────────────────────────────────────
    private DocumentDto.DocumentResponse toDocumentResponse(Document doc) {
        int totalSigs   = doc.getSignatures() != null ? doc.getSignatures().size() : 0;
        int pendingSigs = (int) (doc.getSignatures() != null
                ? doc.getSignatures().stream()
                     .filter(s -> s.getStatus() == com.signatureapp.model.Signature.SignatureStatus.PENDING)
                     .count() : 0);
        int doneSigs    = totalSigs - pendingSigs;

        return DocumentDto.DocumentResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .originalFileName(doc.getOriginalFileName())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus().name())
                .ownerName(doc.getOwner() != null ? doc.getOwner().getName() : null)
                .ownerEmail(doc.getOwner() != null ? doc.getOwner().getEmail() : null)
                .createdAt(doc.getCreatedAt())
                .updatedAt(doc.getUpdatedAt())
                .downloadUrl(fileStorageService.getDownloadUrl(doc.getStoredFileName(), baseUrl))
                .signedDownloadUrl(doc.getSignedFilePath() != null
                        ? fileStorageService.getSignedDownloadUrl(
                            fileStorageService.generateSignedFileName(doc.getStoredFileName()), baseUrl)
                        : null)
                .totalSignatures(totalSigs)
                .pendingSignatures(pendingSigs)
                .completedSignatures(doneSigs)
                .build();
    }

    private DocumentDto.DocumentListResponse toListResponse(Document doc) {
        int totalSigs  = doc.getSignatures() != null ? doc.getSignatures().size() : 0;
        int pendingSigs = (int) (doc.getSignatures() != null
                ? doc.getSignatures().stream()
                     .filter(s -> s.getStatus() == com.signatureapp.model.Signature.SignatureStatus.PENDING)
                     .count() : 0);
        return DocumentDto.DocumentListResponse.builder()
                .id(doc.getId())
                .title(doc.getTitle())
                .originalFileName(doc.getOriginalFileName())
                .fileSize(doc.getFileSize())
                .status(doc.getStatus().name())
                .createdAt(doc.getCreatedAt())
                .totalSignatures(totalSigs)
                .pendingSignatures(pendingSigs)
                .build();
    }

    private Document.DocumentStatus parseStatus(String status) {
        try {
            return Document.DocumentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid status value: " + status);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
