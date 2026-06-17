package com.signatureapp.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@signatureapp.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /**
     * Send signing request email with the public signing URL.
     */
    @Async
    public void sendSigningRequestEmail(String toEmail, String signerName,
                                        String documentTitle, String ownerName,
                                        String signingToken) {
        String signingUrl = baseUrl + "/api/public/sign/" + signingToken;

        String subject = "📄 Document Signature Request: " + documentTitle;
        String body = buildSigningRequestHtml(signerName, documentTitle, ownerName, signingUrl);

        sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Send notification when document is fully signed.
     */
    @Async
    public void sendDocumentSignedEmail(String toEmail, String ownerName, String documentTitle) {
        String subject = "✅ Document Signed: " + documentTitle;
        String body = """
                <div style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #1e3c72;">✅ Your Document Has Been Signed</h2>
                    <p>Hi %s,</p>
                    <p>Great news! Your document <strong>"%s"</strong> has been successfully signed.</p>
                    <p>Log in to download the signed PDF.</p>
                    <br>
                    <p style="color: #666;">— Document Signature App</p>
                </div>
                """.formatted(ownerName, documentTitle);

        sendHtmlEmail(toEmail, subject, body);
    }

    /**
     * Send rejection notification.
     */
    @Async
    public void sendSignatureRejectedEmail(String toEmail, String ownerName,
                                           String documentTitle, String rejectionReason) {
        String subject = "❌ Signature Rejected: " + documentTitle;
        String body = """
                <div style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #d32f2f;">❌ Signature Rejected</h2>
                    <p>Hi %s,</p>
                    <p>A signer has rejected signing <strong>"%s"</strong>.</p>
                    <p><strong>Reason:</strong> %s</p>
                    <p>Please log in to review and take action.</p>
                    <br>
                    <p style="color: #666;">— Document Signature App</p>
                </div>
                """.formatted(ownerName, documentTitle,
                rejectionReason != null ? rejectionReason : "No reason provided");

        sendHtmlEmail(toEmail, subject, body);
    }

    // ─── Internal Helpers ────────────────────────────────────

    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to: {} | Subject: {}", to, subject);
        } catch (MessagingException | MailException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            // Don't throw — email failure should not break the main flow
        }
    }

    private String buildSigningRequestHtml(String signerName, String documentTitle,
                                           String ownerName, String signingUrl) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #1e3c72, #2a5298); padding: 20px; border-radius: 8px 8px 0 0;">
                        <h1 style="color: white; margin: 0;">📄 Document Signature App</h1>
                    </div>
                    <div style="background: #f9f9f9; padding: 30px; border-radius: 0 0 8px 8px; border: 1px solid #ddd;">
                        <h2 style="color: #1e3c72;">You have a document to sign</h2>
                        <p>Hi <strong>%s</strong>,</p>
                        <p><strong>%s</strong> has requested your signature on:</p>
                        <p style="font-size: 18px; color: #333;"><strong>📄 %s</strong></p>
                        <div style="margin: 30px 0; text-align: center;">
                            <a href="%s"
                               style="background: #1e3c72; color: white; padding: 14px 30px;
                                      text-decoration: none; border-radius: 6px; font-size: 16px;
                                      font-weight: bold; display: inline-block;">
                                ✍️ Sign Document
                            </a>
                        </div>
                        <p style="color: #888; font-size: 13px;">
                            This link will expire in 72 hours. If you did not expect this request, please ignore this email.
                        </p>
                        <hr style="border: none; border-top: 1px solid #ddd;">
                        <p style="color: #666; font-size: 12px;">— Document Signature App</p>
                    </div>
                </div>
                """.formatted(signerName, ownerName, documentTitle, signingUrl);
    }
}
