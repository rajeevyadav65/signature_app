package com.signatureapp.controller;

import com.signatureapp.dto.ApiResponse;
import com.signatureapp.dto.DocumentDto;
import com.signatureapp.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final DocumentService documentService;

    /**
     * GET /api/public/sign/{token}
     * Access a document via a public signing token.
     * No authentication required.
     */
    @GetMapping("/sign/{token}")
    public ResponseEntity<ApiResponse<DocumentDto.DocumentResponse>> getDocumentByToken(
            @PathVariable String token,
            HttpServletRequest request) {

        DocumentDto.DocumentResponse doc = documentService.getDocumentByToken(token, request);
        return ResponseEntity.ok(ApiResponse.success("Document retrieved for signing", doc));
    }

    /**
     * GET /api/public/health
     * Public health check endpoint.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> health() {
        return ResponseEntity.ok(ApiResponse.success("Service is running", "OK"));
    }
}
