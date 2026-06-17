package com.signatureapp.service;

import com.signatureapp.config.FileStorageConfig;
import com.signatureapp.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileStorageConfig fileStorageConfig;

    /**
     * Store an uploaded file and return the stored filename.
     */
    public String storeFile(MultipartFile file) {
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "document");

        // Validate extension
        if (!isPdf(originalName)) {
            throw new BadRequestException("Only PDF files are allowed. Got: " + originalName);
        }

        String storedName = UUID.randomUUID() + "_" + originalName;
        Path uploadPath = getUploadPath();

        try {
            Path targetLocation = uploadPath.resolve(storedName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            log.info("Stored file: {} -> {}", originalName, storedName);
            return storedName;
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file: " + originalName, ex);
        }
    }

    /**
     * Get absolute file path for a stored filename.
     */
    public String getFilePath(String storedFileName) {
        return getUploadPath().resolve(storedFileName).toAbsolutePath().toString();
    }

    /**
     * Get absolute file path for a signed PDF.
     */
    public String getSignedFilePath(String storedFileName) {
        return getUploadPath().resolve("signed").resolve(storedFileName)
                .toAbsolutePath().toString();
    }

    /**
     * Generate the path for the signed PDF output file.
     */
    public String generateSignedFileName(String originalStoredName) {
        return "signed_" + originalStoredName;
    }

    /**
     * Delete a file by stored name.
     */
    public void deleteFile(String storedFileName) {
        try {
            Path filePath = getUploadPath().resolve(storedFileName);
            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", storedFileName);
        } catch (IOException e) {
            log.error("Could not delete file: {}", storedFileName, e);
        }
    }

    /**
     * Check if a file exists.
     */
    public boolean fileExists(String storedFileName) {
        return Files.exists(getUploadPath().resolve(storedFileName));
    }

    /**
     * Get download URL for a file.
     */
    public String getDownloadUrl(String storedFileName, String baseUrl) {
        return baseUrl + "/uploads/" + storedFileName;
    }

    /**
     * Get download URL for a signed file.
     */
    public String getSignedDownloadUrl(String storedFileName, String baseUrl) {
        return baseUrl + "/uploads/signed/" + storedFileName;
    }

    private Path getUploadPath() {
        return Paths.get(fileStorageConfig.getUploadDir()).toAbsolutePath().normalize();
    }

    private boolean isPdf(String filename) {
        return filename != null &&
               filename.toLowerCase().endsWith(".pdf");
    }
}
