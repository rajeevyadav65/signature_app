package com.signatureapp.controller;

import com.signatureapp.dto.ApiResponse;
import com.signatureapp.dto.DocumentDto;
import com.signatureapp.dto.SignatureDto;
import com.signatureapp.security.UserDetailsImpl;
import com.signatureapp.service.SignatureService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/signatures")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;

    /**
     * POST /api/signatures
     * Place a signature field on a document (owner defines where signers should sign).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SignatureDto.SignatureResponse>> placeSignature(
            @Valid @RequestBody SignatureDto.PlaceSignatureRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {

        SignatureDto.SignatureResponse response =
                signatureService.placeSignature(request, currentUser, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Signature field placed", response));
    }

    /**
     * GET /api/signatures/{docId}
     * Get all signature fields for a document.
     */
    @GetMapping("/{docId}")
    public ResponseEntity<ApiResponse<List<SignatureDto.SignatureResponse>>> getSignatures(
            @PathVariable Long docId,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<SignatureDto.SignatureResponse> signatures =
                signatureService.getSignaturesForDocument(docId, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Signatures retrieved", signatures));
    }

    /**
     * POST /api/signatures/{signatureId}/sign
     * Sign a signature field (authenticated signer).
     */
    @PostMapping("/{signatureId}/sign")
    public ResponseEntity<ApiResponse<SignatureDto.SignatureResponse>> signDocument(
            @PathVariable Long signatureId,
            @RequestBody SignatureDto.SubmitSignatureRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {

        SignatureDto.SignatureResponse response =
                signatureService.signDocument(signatureId, request, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Document signed successfully", response));
    }

    /**
     * POST /api/signatures/{signatureId}/reject
     * Reject a signature request.
     */
    @PostMapping("/{signatureId}/reject")
    public ResponseEntity<ApiResponse<SignatureDto.SignatureResponse>> rejectSignature(
            @PathVariable Long signatureId,
            @RequestBody SignatureDto.RejectSignatureRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {

        SignatureDto.SignatureResponse response =
                signatureService.rejectSignature(signatureId, request, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Signature rejected", response));
    }

    /**
     * POST /api/signatures/finalize
     * Manually trigger signed PDF generation for a document.
     */
    @PostMapping("/finalize")
    public ResponseEntity<ApiResponse<DocumentDto.DocumentResponse>> finalizeDocument(
            @RequestBody SignatureDto.FinalizeRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {

        DocumentDto.DocumentResponse response =
                signatureService.finalizeDocument(request.getDocumentId(), currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Signed PDF generated successfully", response));
    }

    /**
     * POST /api/signatures/public/{token}/{signatureId}/sign
     * Sign a document via a public token link (no auth required).
     */
    @PostMapping("/public/{token}/{signatureId}/sign")
    public ResponseEntity<ApiResponse<SignatureDto.SignatureResponse>> signDocumentPublic(
            @PathVariable String token,
            @PathVariable Long signatureId,
            @RequestBody SignatureDto.SubmitSignatureRequest request,
            HttpServletRequest httpRequest) {

        SignatureDto.SignatureResponse response =
                signatureService.signDocumentPublic(signatureId, token, request, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Document signed successfully", response));
    }
}
