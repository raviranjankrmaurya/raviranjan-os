package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.Notification;
import com.raviranjan.os.dto.notification.NotificationDto;
import com.raviranjan.os.repository.NotificationRepository;
import com.raviranjan.os.service.impl.NotificationCenterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCenterServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationCenterServiceImpl notificationService;

    private Notification sampleNotification;

    @BeforeEach
    void setUp() {
        sampleNotification = new Notification();
        sampleNotification.setId(UUID.randomUUID());
        sampleNotification.setTitle("New Contact Inquiry");
        sampleNotification.setMessage("Recruiter inquiry from TechCorp");
        sampleNotification.setPriority("URGENT");
        sampleNotification.setType("CRM");
        sampleNotification.setRead(false);
    }

    @Test
    @DisplayName("Should return paginated notifications")
    void testGetNotifications() {
        Pageable pageable = PageRequest.of(0, 10);
        when(notificationRepository.findAllByOrderByCreatedAtDesc(pageable))
                .thenReturn(new PageImpl<>(List.of(sampleNotification)));

        PageResponse<NotificationDto> result = notificationService.getNotifications(null, null, null, pageable);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("New Contact Inquiry", result.getContent().get(0).getTitle());
        assertEquals("URGENT", result.getContent().get(0).getPriority());
    }

    @Test
    @DisplayName("Should mark single notification as read")
    void testMarkAsRead() {
        UUID id = sampleNotification.getId();
        when(notificationRepository.findById(id)).thenReturn(Optional.of(sampleNotification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(i -> i.getArgument(0));

        NotificationDto result = notificationService.markAsRead(id);

        assertNotNull(result);
        assertTrue(result.isRead());
        verify(notificationRepository, times(1)).save(sampleNotification);
    }

    @Test
    @DisplayName("Should return accurate unread notification count")
    void testGetUnreadCount() {
        when(notificationRepository.countByReadFalse()).thenReturn(5L);

        long count = notificationService.getUnreadCount();

        assertEquals(5L, count);
    }
}
