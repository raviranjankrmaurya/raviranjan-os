package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.dto.notification.NotificationDto;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface NotificationCenterService {
    PageResponse<NotificationDto> getNotifications(Boolean isRead, String priority, String type, Pageable pageable);
    NotificationDto markAsRead(UUID id);
    void markAllAsRead();
    long getUnreadCount();
    default void deleteNotification(UUID id) {}
}
