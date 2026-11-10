package com.raviranjan.os.service.security;

import com.raviranjan.os.dto.crm.ContactInquiryRequest;
import com.raviranjan.os.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Server-side Contact Inquiry Validator & Sanitizer.
 * Enforces Unicode name rules (2-80 chars), email syntax & length, disposable domain detection,
 * message bounds, and HTML escaping against Stored XSS.
 */
@Component
public class ContactValidator {

    // Allows Unicode letters, combining marks, spaces, apostrophes (' and ’), hyphens (-), and periods (.)
    private static final Pattern VALID_NAME_PATTERN =
            Pattern.compile("^[\\p{L}\\p{M}][\\p{L}\\p{M}\\s'’\\-.]{1,79}$");

    private static final Pattern HTML_TAG_PATTERN =
            Pattern.compile("<\\s*/?\\s*[a-zA-Z!][^>]*>");

    private static final Pattern EMAIL_SYNTAX_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}$");

    private static final Set<String> DISPOSABLE_EMAIL_DOMAINS = Set.of(
            "mailinator.com",
            "10minutemail.com",
            "guerrillamail.com",
            "tempmail.com",
            "temp-mail.org",
            "yopmail.com",
            "trashmail.com",
            "sharklasers.com",
            "getnada.com",
            "maildrop.cc",
            "dispostable.com",
            "fakeinbox.com",
            "throwawaymail.com"
    );

    public record ValidatedContactInput(
            String normalizedName,
            String normalizedEmail,
            String sanitizedCompany,
            String sanitizedSubject,
            String originalMessage,
            String escapedMessage,
            boolean disposableEmailDomain,
            String messageSha256
    ) {}

    public ValidatedContactInput validateAndNormalize(ContactInquiryRequest request) {
        if (request == null) {
            throw new BadRequestException("Contact inquiry payload is required.");
        }

        // 1. Validate Name (2 to 80 characters, Unicode letters, spaces, apostrophes, hyphens)
        String rawName = request.getName();
        if (rawName == null || rawName.isBlank()) {
            throw new BadRequestException("Name is required.");
        }
        String trimmedName = rawName.trim().replaceAll("\\s+", " ");
        if (trimmedName.length() < 2 || trimmedName.length() > 80) {
            throw new BadRequestException("Name must be between 2 and 80 characters.");
        }
        if (HTML_TAG_PATTERN.matcher(trimmedName).find() || containsControlChars(trimmedName)) {
            throw new BadRequestException("Name contains invalid or disallowed characters.");
        }
        if (!VALID_NAME_PATTERN.matcher(trimmedName).matches() || countUnicodeLetters(trimmedName) < 2) {
            throw new BadRequestException("Name must contain valid letters (2 to 80 characters).");
        }

        // 2. Validate & Normalize Email
        String rawEmail = request.getEmail();
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new BadRequestException("Email is required.");
        }
        String normalizedEmail = rawEmail.trim().toLowerCase(Locale.ROOT);
        if (normalizedEmail.length() < 5 || normalizedEmail.length() > 254) {
            throw new BadRequestException("Email address length must be between 5 and 254 characters.");
        }
        if (normalizedEmail.contains("..") || normalizedEmail.contains(" ") || !EMAIL_SYNTAX_PATTERN.matcher(normalizedEmail).matches()) {
            throw new BadRequestException("Please provide a syntactically valid email address.");
        }
        String domain = normalizedEmail.substring(normalizedEmail.indexOf('@') + 1);
        if (domain.startsWith("-") || domain.endsWith("-") || domain.startsWith(".")) {
            throw new BadRequestException("Email domain is malformed.");
        }
        boolean disposable = DISPOSABLE_EMAIL_DOMAINS.contains(domain);

        // 3. Validate Message
        String rawMessage = request.getMessage();
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new BadRequestException("Message content is required.");
        }
        String trimmedMessage = rawMessage.trim();
        if (trimmedMessage.length() < 2 || trimmedMessage.length() > 5000) {
            throw new BadRequestException("Message must be between 2 and 5000 characters.");
        }

        String company = request.getCompany() != null ? request.getCompany().trim() : null;
        if (company != null && company.length() > 150) {
            company = company.substring(0, 150);
        }

        String subject = request.getSubject() != null && !request.getSubject().isBlank()
                ? request.getSubject().trim()
                : "Direct Portfolio / Recruiter Inquiry";
        if (subject.length() > 255) {
            subject = subject.substring(0, 255);
        }

        return new ValidatedContactInput(
                trimmedName,
                normalizedEmail,
                company,
                subject,
                trimmedMessage,
                escapeHtml(trimmedMessage),
                disposable,
                sha256Hex(normalizedEmail + "|" + trimmedMessage.toLowerCase(Locale.ROOT).replaceAll("\\s+", " "))
        );
    }

    public String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    public String hashBrowserIdentifier(String rawBrowserId, String userAgent) {
        if (rawBrowserId == null || rawBrowserId.isBlank() || "OPT_OUT".equalsIgnoreCase(rawBrowserId.trim())) {
            return null;
        }
        String clean = rawBrowserId.trim();
        if (clean.length() > 128) {
            clean = clean.substring(0, 128);
        }
        return sha256Hex("bhash:" + clean + ":" + (userAgent != null ? userAgent.substring(0, Math.min(80, userAgent.length())) : ""));
    }

    private int countUnicodeLetters(String s) {
        int count = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isLetter(s.charAt(i))) {
                count++;
            }
        }
        return count;
    }

    private boolean containsControlChars(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isISOControl(c)) {
                return true;
            }
        }
        return false;
    }

    public String sha256Hex(String value) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
