package com.raviranjan.os.service.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Value("${app.notifications.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${app.notifications.email.admin:admin@raviranjan.dev}")
    private String adminEmail;

    public boolean sendInquiryAlert(String senderName, String senderEmail, String subject, String message) {
        if (!emailEnabled) {
            log.info("Email notifications disabled. Skipping email alert for inquiry from: {}", senderEmail);
            return false;
        }

        log.info("[EMAIL DISPATCH] To: {} | Subject: [URGENT] New Inquiry from {} - {}", 
                adminEmail, senderName, subject != null ? subject : "Contact Form");
        log.info("[EMAIL CONTENT] Body: {}", message);
        return true;
    }

    public boolean sendIntegrationFailureAlert(String provider, String error) {
        if (!emailEnabled) return false;
        log.warn("[EMAIL DISPATCH] To: {} | Subject: [ALERT] Integration Failure on {}", adminEmail, provider);
        log.warn("[EMAIL CONTENT] Error: {}", error);
        return true;
    }

    public boolean sendMilestoneAlert(String projectTitle, String milestoneTitle) {
        if (!emailEnabled) return false;
        log.info("[EMAIL DISPATCH] To: {} | Subject: Milestone Completed: {} - {}", adminEmail, projectTitle, milestoneTitle);
        return true;
    }
}
