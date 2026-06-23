package com.signatureapp.service;

import com.signatureapp.dto.DocumentDto;
import com.signatureapp.dto.SignatureDto;
import com.signatureapp.exception.BadRequestException;
import com.signatureapp.exception.ResourceNotFoundException;
import com.signatureapp.exception.UnauthorizedException;
import com.signatureapp.model.AuditLog;
import com.signatureapp.model.Document;
import com.signatureapp.model.Signature;
import com.signatureapp.model.User;
import com.signatureapp.repository.DocumentRepository;
import com.signatureapp.repository.SignatureRepository;
import com.signatureapp.repository.UserRepository;
import com.signatureapp.security.UserDetailsImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignatureService {

    private final SignatureRepository signatureRepository;
    private final DocumentRepository documentRepository;
    private final UserRepository userRepository;
    private final PdfService pdfService;
    private final FileStorageService fileStorageService;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    // ─── Place a Signature Field on a Document ────────────────
    @Transactional
    public SignatureDto.SignatureResponse placeSignature(
            SignatureDto.PlaceSignatureRequest request,
            UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {

        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new ResourceNotFoundException("Document", request.getDocumentId()));

        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not own this document");
        }

        if (document.getStatus() == Document.DocumentStatus.SIGNED) {
            throw new BadRequestException("Document is already fully signed");
        }

        User owner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        Signature signature = Signature.builder()
                .document(document)
                .signerName(request.getSignerName())
                .signerEmail(request.getSignerEmail())
                .xCoordinate(request.getXCoordinate())
                .yCoordinate(request.getYCoordinate())
                .pageNumber(request.getPageNumber())
                .width(request.getWidth() != null ? request.getWidth() : 150f)
                .height(request.getHeight() != null ? request.getHeight() : 50f)
                .status(Signature.SignatureStatus.PENDING)
                .build();

        signature = signatureRepository.save(signature);

        auditLogService.logAction(document, owner, AuditLog.AuditAction.SIGNATURE_PLACED,
                "Signature field placed for: " + request.getSignerEmail()
                        + " on page " + request.getPageNumber(),
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

        log.info("Signature placed on doc {} for {}", document.getId(), request.getSignerEmail());
        return toResponse(signature);
    }

    // ─── Submit / Perform Signing (Authenticated) ─────────────
    @Transactional
    public SignatureDto.SignatureResponse signDocument(
            Long signatureId,
            SignatureDto.SubmitSignatureRequest request,
            UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {

        Signature signature = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Signature", signatureId));

        if (signature.getStatus() != Signature.SignatureStatus.PENDING) {
            throw new BadRequestException("Signature is already " + signature.getStatus().name().toLowerCase());
        }

        User signer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        // Validate signer email matches
        if (!signer.getEmail().equalsIgnoreCase(signature.getSignerEmail())) {
            throw new UnauthorizedException("You are not the intended signer for this signature field");
        }

        signature.setSigner(signer);
        signature.setSignatureData(request.getSignatureData());
        signature.setSignatureType(request.getSignatureType() != null ? request.getSignatureType() : "DRAWN");
        signature.setSignerIpAddress(getClientIp(httpRequest));
        signature.setStatus(Signature.SignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now());

        signature = signatureRepository.save(signature);

        auditLogService.logAction(signature.getDocument(), signer,
                AuditLog.AuditAction.SIGNATURE_SIGNED,
                "Document signed by: " + signer.getEmail(),
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

        // Check if all signatures are done → auto-finalize
        checkAndFinalizeDocument(signature.getDocument(), httpRequest);

        return toResponse(signature);
    }

    // ─── Submit Signing via Public Token ──────────────────────
    @Transactional
    public SignatureDto.SignatureResponse signDocumentPublic(
            Long signatureId,
            String token,
            SignatureDto.SubmitSignatureRequest request,
            HttpServletRequest httpRequest) {

        Signature signature = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Signature", signatureId));

        Document document = signature.getDocument();

        // Validate token
        if (!token.equals(document.getSigningToken())) {
            throw new UnauthorizedException("Invalid signing token");
        }
        if (document.getSigningTokenExpiry() != null
                && document.getSigningTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Signing link has expired");
        }

        if (signature.getStatus() != Signature.SignatureStatus.PENDING) {
            throw new BadRequestException("Signature is already " + signature.getStatus().name().toLowerCase());
        }

        String signerName  = request.getSignerName()  != null ? request.getSignerName()  : signature.getSignerName();
        String signerEmail = request.getSignerEmail() != null ? request.getSignerEmail() : signature.getSignerEmail();

        signature.setSignatureData(request.getSignatureData());
        signature.setSignatureType(request.getSignatureType() != null ? request.getSignatureType() : "DRAWN");
        signature.setSignerIpAddress(getClientIp(httpRequest));
        signature.setSignerName(signerName);
        signature.setSignerEmail(signerEmail);
        signature.setStatus(Signature.SignatureStatus.SIGNED);
        signature.setSignedAt(LocalDateTime.now());

        signature = signatureRepository.save(signature);

        auditLogService.logAction(document, signerName, signerEmail,
                AuditLog.AuditAction.SIGNATURE_SIGNED,
                "Document signed (public) by: " + signerEmail,
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

        checkAndFinalizeDocument(document, httpRequest);

        return toResponse(signature);
    }

    // ─── Reject a Signature ───────────────────────────────────
    @Transactional
    public SignatureDto.SignatureResponse rejectSignature(
            Long signatureId,
            SignatureDto.RejectSignatureRequest request,
            UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {

        Signature signature = signatureRepository.findById(signatureId)
                .orElseThrow(() -> new ResourceNotFoundException("Signature", signatureId));

        if (signature.getStatus() != Signature.SignatureStatus.PENDING) {
            throw new BadRequestException("Signature is not in PENDING state");
        }

        User signer = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        if (!signer.getEmail().equalsIgnoreCase(signature.getSignerEmail())) {
            throw new UnauthorizedException("You are not the intended signer");
        }

        signature.setStatus(Signature.SignatureStatus.REJECTED);
        signature.setRejectionReason(request.getRejectionReason());
        signature = signatureRepository.save(signature);

        // Mark document as REJECTED
        Document document = signature.getDocument();
        document.setStatus(Document.DocumentStatus.REJECTED);
        documentRepository.save(document);

        auditLogService.logAction(document, signer, AuditLog.AuditAction.SIGNATURE_REJECTED,
                "Signature rejected. Reason: " + request.getRejectionReason(),
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

        // Notify document owner
        User owner = document.getOwner();
        emailService.sendSignatureRejectedEmail(
                owner.getEmail(), owner.getName(), document.getTitle(),
                request.getRejectionReason());

        return toResponse(signature);
    }

    // ─── Get Signatures for a Document ───────────────────────
    @Transactional(readOnly = true)
    public List<SignatureDto.SignatureResponse> getSignaturesForDocument(
            Long documentId, UserDetailsImpl currentUser) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not own this document");
        }

        return signatureRepository.findByDocumentOrderByCreatedAtAsc(document)
                .stream().map(this::toResponse).toList();
    }

    // ─── Manually Finalize / Generate Signed PDF ─────────────
    @Transactional
    public DocumentDto.DocumentResponse finalizeDocument(
            Long documentId,
            UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {

        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("Document", documentId));

        if (!document.getOwner().getId().equals(currentUser.getId())) {
            throw new UnauthorizedException("You do not own this document");
        }

        User owner = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", currentUser.getId()));

        return generateSignedPdf(document, owner, httpRequest);
    }

    // ─── Internal: auto-finalize when all sigs are done ──────
    private void checkAndFinalizeDocument(Document document, HttpServletRequest httpRequest) {
        List<Signature> allSigs = signatureRepository.findByDocumentOrderByCreatedAtAsc(document);
        if (allSigs.isEmpty()) return;

        boolean allSigned = allSigs.stream()
                .allMatch(s -> s.getStatus() == Signature.SignatureStatus.SIGNED);

        if (allSigned) {
            generateSignedPdf(document, document.getOwner(), httpRequest);
        }
    }

    private DocumentDto.DocumentResponse generateSignedPdf(
            Document document, User actor, HttpServletRequest httpRequest) {

        List<Signature> signatures = signatureRepository.findByDocumentOrderByCreatedAtAsc(document);
        List<Signature> signedSigs = signatures.stream()
                .filter(s -> s.getStatus() == Signature.SignatureStatus.SIGNED)
                .toList();

        if (signedSigs.isEmpty()) {
            throw new BadRequestException("No signed signatures to embed in the PDF");
        }

        String signedFileName = fileStorageService.generateSignedFileName(document.getStoredFileName());
        String signedFilePath = fileStorageService.getSignedFilePath(signedFileName);

        try {
            pdfService.embedSignaturesAndSave(document.getFilePath(), signedFilePath, signedSigs);
        } catch (IOException e) {
            log.error("Failed to generate signed PDF: {}", e.getMessage());
            throw new RuntimeException("Failed to generate signed PDF: " + e.getMessage());
        }

        document.setSignedFilePath(signedFilePath);
        document.setStatus(Document.DocumentStatus.SIGNED);
        document = documentRepository.save(document);

        auditLogService.logAction(document, actor, AuditLog.AuditAction.SIGNED_PDF_GENERATED,
                "Signed PDF generated with " + signedSigs.size() + " signature(s)",
                getClientIp(httpRequest), httpRequest.getHeader("User-Agent"));

        // Notify owner
        emailService.sendDocumentSignedEmail(
                document.getOwner().getEmail(),
                document.getOwner().getName(),
                document.getTitle());

        log.info("Signed PDF generated for document: {}", document.getId());
        return toDocumentResponse(document);
    }

    // ─── Mappers ──────────────────────────────────────────────
    private SignatureDto.SignatureResponse toResponse(Signature sig) {
        return SignatureDto.SignatureResponse.builder()
                .id(sig.getId())
                .documentId(sig.getDocument().getId())
                .documentTitle(sig.getDocument().getTitle())
                .signerName(sig.getSignerName())
                .signerEmail(sig.getSignerEmail())
                .xCoordinate(sig.getXCoordinate())
                .yCoordinate(sig.getYCoordinate())
                .pageNumber(sig.getPageNumber())
                .width(sig.getWidth())
                .height(sig.getHeight())
                .status(sig.getStatus().name())
                .rejectionReason(sig.getRejectionReason())
                .signedAt(sig.getSignedAt())
                .createdAt(sig.getCreatedAt())
                .build();
    }

    private DocumentDto.DocumentResponse toDocumentResponse(Document doc) {
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
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xf = request.getHeader("X-Forwarded-For");
        return (xf == null || xf.isEmpty()) ? request.getRemoteAddr() : xf.split(",")[0].trim();
    }
}