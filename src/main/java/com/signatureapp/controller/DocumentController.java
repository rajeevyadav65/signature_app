package com.signatureapp.controller;

import com.signatureapp.dto.ApiResponse;
import com.signatureapp.dto.DocumentDto;
import com.signatureapp.security.UserDetailsImpl;
import com.signatureapp.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/docs")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    /**
     * POST /api/docs/upload
     * Upload a PDF document. Accepts multipart/form-data.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentDto.DocumentResponse>> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "title", required = false) String title,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            HttpServletRequest request) {

        DocumentDto.DocumentResponse response =
                documentService.uploadDocument(file, title, currentUser, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Document uploaded successfully", response));
    }

    /**
     * GET /api/docs
     * List all documents for the current user.
     * Optional ?status=PENDING|SIGNED|REJECTED filter.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<DocumentDto.DocumentListResponse>>> getMyDocuments(
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        List<DocumentDto.DocumentListResponse> docs =
                documentService.getMyDocuments(currentUser, status);
        return ResponseEntity.ok(ApiResponse.success("Documents retrieved", docs));
    }

    /**
     * GET /api/docs/{id}
     * Get details of a specific document.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DocumentDto.DocumentResponse>> getDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            HttpServletRequest request) {

        DocumentDto.DocumentResponse doc =
                documentService.getDocumentById(id, currentUser, request);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved", doc));
    }

    /**
     * POST /api/docs/{id}/signing-link
     * Generate a shareable public signing link for a document.
     */
    @PostMapping("/{id}/signing-link")
    public ResponseEntity<ApiResponse<DocumentDto.SigningLinkResponse>> generateSigningLink(
            @PathVariable Long id,
            @RequestBody(required = false) DocumentDto.GenerateSigningLinkRequest request,
            @AuthenticationPrincipal UserDetailsImpl currentUser,
            HttpServletRequest httpRequest) {

        DocumentDto.SigningLinkResponse response =
                documentService.generateSigningLink(id, request, currentUser, httpRequest);
        return ResponseEntity.ok(ApiResponse.success("Signing link generated", response));
    }

    /**
     * GET /api/docs/dashboard
     * Get dashboard statistics for the current user.
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<DocumentDto.DashboardStats>> getDashboard(
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        DocumentDto.DashboardStats stats = documentService.getDashboardStats(currentUser);
        return ResponseEntity.ok(ApiResponse.success("Dashboard stats retrieved", stats));
    }

    /**
     * DELETE /api/docs/{id}
     * Delete a document (only owner can delete).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl currentUser) {

        documentService.deleteDocument(id, currentUser);
        return ResponseEntity.ok(ApiResponse.success("Document deleted successfully", null));
    }
}
