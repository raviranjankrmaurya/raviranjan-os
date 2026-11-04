package com.raviranjan.os.realtime;

import com.raviranjan.os.domain.entity.Notification;
import com.raviranjan.os.event.DomainEvent;
import com.raviranjan.os.event.DomainEventType;
import com.raviranjan.os.repository.NotificationRepository;
import com.raviranjan.os.service.notification.EmailNotificationService;
import com.raviranjan.os.service.notification.WhatsAppNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class GlobalDomainEventListener {

    private static final Logger log = LoggerFactory.getLogger(GlobalDomainEventListener.class);

    private final NotificationRepository notificationRepository;
    private final EmailNotificationService emailService;
    private final WhatsAppNotificationService whatsAppService;
    private final AdminSseEmitterRegistry adminSseRegistry;
    private final SseEmitterRegistry publicSseRegistry;
    private final StringRedisTemplate redisTemplate;

    public GlobalDomainEventListener(
            NotificationRepository notificationRepository,
            EmailNotificationService emailService,
            WhatsAppNotificationService whatsAppService,
            AdminSseEmitterRegistry adminSseRegistry,
            SseEmitterRegistry publicSseRegistry,
            StringRedisTemplate redisTemplate
    ) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
        this.whatsAppService = whatsAppService;
        this.adminSseRegistry = adminSseRegistry;
        this.publicSseRegistry = publicSseRegistry;
        this.redisTemplate = redisTemplate;
    }

    @EventListener
    @Transactional
    public void handleDomainEvent(DomainEvent event) {
        if (event == null) return;
        try {
            log.info("Processing DomainEvent: {} [{}]", event.getEventType(), event.getTitle());

            // 1. Persist Admin Notification in PostgreSQL
            Notification notification = new Notification();
            notification.setTitle(event.getTitle() != null ? event.getTitle() : "System Notification");
            notification.setMessage(event.getMessage() != null ? event.getMessage() : "");
            notification.setPriority(event.getPriority() != null ? event.getPriority() : "MEDIUM");
            notification.setLevel("URGENT".equalsIgnoreCase(event.getPriority()) ? "ALERT" : "INFO");
            notification.setType(event.getEventType() != null ? event.getEventType().name() : "SYSTEM");
            notification.setRelatedEntityType(event.getRelatedEntityType());
            notification.setRelatedEntityId(event.getRelatedEntityId());
            notification.setActionUrl(event.getActionUrl() != null ? event.getActionUrl() : "/admin");
            notification.setRead(false);

            // 2. Dispatch Email / WhatsApp safely when applicable
            try {
                if (event.getEventType() == DomainEventType.NEW_CONTACT_INQUIRY) {
                    boolean emailed = emailService.sendInquiryAlert("Visitor", "crm@raviranjan.dev", notification.getTitle(), notification.getMessage());
                    notification.setEmailDispatched(emailed);

                    boolean whatsApped = whatsAppService.sendAdminWhatsAppAlert(
                            "[Raviranjan.OS] " + notification.getTitle() + " - " + notification.getMessage(),
                            notification.getPriority()
                    );
                    notification.setWhatsappDispatched(whatsApped);
                } else if (event.getEventType() == DomainEventType.INTEGRATION_FAILED) {
                    boolean emailed = emailService.sendIntegrationFailureAlert(event.getRelatedEntityId(), notification.getMessage());
                    notification.setEmailDispatched(emailed);
                } else if (event.getEventType() == DomainEventType.MILESTONE_COMPLETED) {
                    boolean emailed = emailService.sendMilestoneAlert(notification.getTitle(), notification.getMessage());
                    notification.setEmailDispatched(emailed);
                }
            } catch (Exception dispatchEx) {
                log.debug("External alert dispatch skipped: {}", dispatchEx.getMessage());
            }

            Notification saved = notificationRepository.save(notification);

            // 3. Publish to Redis Event Bus Topic
            try {
                if (redisTemplate != null) {
                    redisTemplate.convertAndSend("raviranjan:events", notification.getType() + ":" + notification.getTitle());
                }
            } catch (Exception e) {
                log.debug("Redis pub/sub broadcast skipped: {}", e.getMessage());
            }

            // 4. Push to Admin SSE Stream using null-safe LinkedHashMap
            try {
                Map<String, Object> ssePayload = new LinkedHashMap<>();
                ssePayload.put("id", saved != null && saved.getId() != null ? saved.getId() : UUID.randomUUID());
                ssePayload.put("title", saved != null && saved.getTitle() != null ? saved.getTitle() : notification.getTitle());
                ssePayload.put("message", saved != null && saved.getMessage() != null ? saved.getMessage() : notification.getMessage());
                ssePayload.put("priority", saved != null && saved.getPriority() != null ? saved.getPriority() : notification.getPriority());
                ssePayload.put("type", saved != null && saved.getType() != null ? saved.getType() : notification.getType());
                ssePayload.put("actionUrl", saved != null && saved.getActionUrl() != null ? saved.getActionUrl() : "/admin");
                ssePayload.put("createdAt", saved != null && saved.getCreatedAt() != null ? saved.getCreatedAt() : Instant.now());
                adminSseRegistry.broadcast("admin-notification", ssePayload);
            } catch (Exception sseEx) {
                log.debug("Admin SSE broadcast skipped: {}", sseEx.getMessage());
            }

            // 5. Broadcast to Public SSE Stream if statistics or milestones changed
            try {
                if (event.getEventType() == DomainEventType.SYNCHRONIZATION_COMPLETED ||
                    event.getEventType() == DomainEventType.GITHUB_CONTRIBUTION_CHANGED ||
                    event.getEventType() == DomainEventType.MILESTONE_COMPLETED) {
                    Map<String, Object> pubPayload = new LinkedHashMap<>();
                    pubPayload.put("eventType", event.getEventType().name());
                    pubPayload.put("title", notification.getTitle());
                    publicSseRegistry.broadcast("statistics-refresh", pubPayload);
                }
            } catch (Exception pubSseEx) {
                log.debug("Public SSE broadcast skipped: {}", pubSseEx.getMessage());
            }
        } catch (Exception ex) {
            log.warn("DomainEvent handling warning for {}: {}", event.getEventType(), ex.getMessage());
        }
    }
}
