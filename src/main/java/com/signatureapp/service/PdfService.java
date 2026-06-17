package com.signatureapp.service;

import com.signatureapp.exception.BadRequestException;
import com.signatureapp.model.Signature;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
public class PdfService {

    /**
     * Embed all signatures into the PDF and save as a new signed file.
     */
    public void embedSignaturesAndSave(String sourcePath, String targetPath,
                                       List<Signature> signatures) throws IOException {
        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            throw new BadRequestException("Source PDF not found: " + sourcePath);
        }

        try (PDDocument doc = Loader.loadPDF(sourceFile)) {
            for (Signature sig : signatures) {
                if (sig.getStatus() != Signature.SignatureStatus.SIGNED) continue;

                int pageIndex = sig.getPageNumber() - 1; // convert 1-indexed to 0-indexed
                if (pageIndex < 0 || pageIndex >= doc.getNumberOfPages()) {
                    log.warn("Skipping signature {} — invalid page {}", sig.getId(), sig.getPageNumber());
                    continue;
                }

                PDPage page = doc.getPage(pageIndex);
                PDRectangle mediaBox = page.getMediaBox();

                try (PDPageContentStream contentStream = new PDPageContentStream(
                        doc, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                    float x = sig.getXCoordinate();
                    float y = sig.getYCoordinate();
                    float width  = sig.getWidth()  != null ? sig.getWidth()  : 150f;
                    float height = sig.getHeight() != null ? sig.getHeight() : 50f;

                    // Flip Y: PDF origin is bottom-left, frontend sends top-left coords
                    float pdfY = mediaBox.getHeight() - y - height;

                    if (sig.getSignatureData() != null && sig.getSignatureData().startsWith("data:image")) {
                        // Embed drawn signature image
                        embedSignatureImage(doc, contentStream, sig.getSignatureData(), x, pdfY, width, height);
                    } else {
                        // Embed typed name as styled text
                        embedSignatureText(contentStream, sig.getSignerName(), x, pdfY, width, height);
                    }

                    // Draw signature border box
                    drawSignatureBox(contentStream, x, pdfY, width, height);

                    // Append metadata line below signature
                    appendSignatureMetadata(contentStream, sig, x, pdfY);
                }
            }

            // Add "SIGNED" watermark on first page
            addSignedWatermark(doc);

            doc.save(targetPath);
            log.info("Signed PDF saved to: {}", targetPath);
        }
    }

    private void embedSignatureImage(PDDocument doc, PDPageContentStream cs,
                                     String base64Data, float x, float y,
                                     float w, float h) throws IOException {
        String base64 = base64Data.contains(",")
                ? base64Data.split(",")[1]
                : base64Data;
        byte[] imgBytes = Base64.getDecoder().decode(base64);
        PDImageXObject image = PDImageXObject.createFromByteArray(doc, imgBytes, "signature");
        cs.drawImage(image, x, y, w, h);
    }

    private void embedSignatureText(PDPageContentStream cs, String signerName,
                                    float x, float y, float w, float h) throws IOException {
        cs.setNonStrokingColor(new Color(30, 60, 114));
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD_OBLIQUE), 18);
        cs.newLineAtOffset(x + 5, y + (h / 2) - 7);
        cs.showText(signerName != null ? signerName : "Signed");
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
    }

    private void drawSignatureBox(PDPageContentStream cs,
                                  float x, float y, float w, float h) throws IOException {
        cs.setStrokingColor(new Color(30, 60, 114));
        cs.setLineWidth(1.0f);
        cs.addRect(x, y, w, h);
        cs.stroke();
        cs.setStrokingColor(Color.BLACK);
    }

    private void appendSignatureMetadata(PDPageContentStream cs, Signature sig,
                                         float x, float y) throws IOException {
        String timestamp = sig.getSignedAt() != null
                ? sig.getSignedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                : LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String metaLine = "Signed by: " + sig.getSignerEmail() + "  |  " + timestamp;
        if (sig.getSignerIpAddress() != null) {
            metaLine += "  |  IP: " + sig.getSignerIpAddress();
        }

        cs.setNonStrokingColor(Color.GRAY);
        cs.beginText();
        cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 6);
        cs.newLineAtOffset(x, y - 9);
        cs.showText(metaLine);
        cs.endText();
        cs.setNonStrokingColor(Color.BLACK);
    }

    private void addSignedWatermark(PDDocument doc) throws IOException {
        PDPage firstPage = doc.getPage(0);
        PDRectangle mediaBox = firstPage.getMediaBox();

        try (PDPageContentStream cs = new PDPageContentStream(
                doc, firstPage, PDPageContentStream.AppendMode.APPEND, true, true)) {

            cs.saveGraphicsState();

            // Semi-transparent green "SIGNED" stamp in top-right corner
            cs.setNonStrokingColor(new Color(0, 150, 0, 60));
            cs.setStrokingColor(new Color(0, 150, 0));
            cs.setLineWidth(2f);

            float stampW = 120f;
            float stampH = 40f;
            float stampX = mediaBox.getWidth() - stampW - 20;
            float stampY = mediaBox.getHeight() - stampH - 20;

            cs.addRect(stampX, stampY, stampW, stampH);
            cs.fillAndStroke();

            cs.setNonStrokingColor(new Color(0, 100, 0));
            cs.beginText();
            cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 22);
            cs.newLineAtOffset(stampX + 10, stampY + 10);
            cs.showText("✓ SIGNED");
            cs.endText();

            cs.restoreGraphicsState();
        }
    }

    /**
     * Get the number of pages in a PDF file.
     */
    public int getPageCount(String filePath) {
        try (PDDocument doc = Loader.loadPDF(new File(filePath))) {
            return doc.getNumberOfPages();
        } catch (IOException e) {
            log.error("Could not read PDF page count: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Validate that the file is a real PDF.
     */
    public boolean isValidPdf(byte[] fileBytes) {
        try (PDDocument doc = Loader.loadPDF(fileBytes)) {
            return doc.getNumberOfPages() > 0;
        } catch (IOException e) {
            return false;
        }
    }
}
