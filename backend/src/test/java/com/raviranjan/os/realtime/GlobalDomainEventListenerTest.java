package com.raviranjan.os.realtime;

import com.raviranjan.os.domain.entity.Notification;
import com.raviranjan.os.event.Events;
import com.raviranjan.os.repository.NotificationRepository;
import com.raviranjan.os.service.notification.EmailNotificationService;
import com.raviranjan.os.service.notification.WhatsAppNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalDomainEventListenerTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private EmailNotificationService emailService;
    @Mock
    private WhatsAppNotificationService whatsAppService;
    @Mock
    private AdminSseEmitterRegistry adminSseRegistry;
    @Mock
    private SseEmitterRegistry publicSseRegistry;
    @Mock
    private StringRedisTemplate redisTemplate;

    @InjectMocks
    private GlobalDomainEventListener eventListener;

    @Test
    @DisplayName("Should handle ContactInquiryEvent, trigger email, WhatsApp, and broadcast via Admin SSE")
    void testHandleContactInquiryEvent() {
        Events.ContactInquiryEvent event = new Events.ContactInquiryEvent(
                this, "Lead Recruiter", "Staff Distributed Systems role", "inq-123", "/admin/crm"
        );

        Notification saved = new Notification();
        saved.setTitle(event.getTitle());
        saved.setMessage(event.getMessage());
        saved.setPriority("URGENT");
        saved.setType("NEW_CONTACT_INQUIRY");

        when(emailService.sendInquiryAlert(anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(whatsAppService.sendAdminWhatsAppAlert(anyString(), anyString())).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        eventListener.handleDomainEvent(event);

        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(emailService, times(1)).sendInquiryAlert(anyString(), anyString(), anyString(), anyString());
        verify(whatsAppService, times(1)).sendAdminWhatsAppAlert(anyString(), anyString());
        verify(adminSseRegistry, times(1)).broadcast(eq("admin-notification"), any());
    }

    @Test
    @DisplayName("Should handle IntegrationFailedEvent and send critical alert email")
    void testHandleIntegrationFailedEvent() {
        Events.IntegrationFailedEvent event = new Events.IntegrationFailedEvent(
                this, "GITHUB", "GitHub API rate limit exceeded", "/admin/github"
        );

        Notification saved = new Notification();
        saved.setTitle(event.getTitle());
        saved.setMessage(event.getMessage());

        when(emailService.sendIntegrationFailureAlert("GITHUB", "GitHub API rate limit exceeded")).thenReturn(true);
        when(notificationRepository.save(any(Notification.class))).thenReturn(saved);

        eventListener.handleDomainEvent(event);

        verify(emailService, times(1)).sendIntegrationFailureAlert("GITHUB", "GitHub API rate limit exceeded");
        verify(adminSseRegistry, times(1)).broadcast(eq("admin-notification"), any());
    }
}
