package com.raviranjan.os.service.impl;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.Notification;
import com.raviranjan.os.dto.notification.NotificationDto;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.repository.NotificationRepository;
import com.raviranjan.os.service.NotificationCenterService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationCenterServiceImpl implements NotificationCenterService {

    private final NotificationRepository notificationRepository;

    public NotificationCenterServiceImpl(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationDto> getNotifications(Boolean isRead, String priority, String type, Pageable pageable) {
        Page<Notification> rawPage = notificationRepository.findAllByOrderByCreatedAtDesc(pageable);
        if (isRead == null && (priority == null || priority.isBlank()) && (type == null || type.isBlank())) {
            return PageResponse.from(rawPage.map(this::toDto));
        }

        List<NotificationDto> filtered = rawPage.getContent().stream()
                .filter(n -> isRead == null || n.isRead() == isRead)
                .filter(n -> {
                    if (priority == null || priority.isBlank() || "ALL".equalsIgnoreCase(priority)) return true;
                    String p = n.getPriority() != null ? n.getPriority() : "";
                    if ("URGENT".equalsIgnoreCase(priority)) {
                        return "URGENT".equalsIgnoreCase(p) || "HIGH".equalsIgnoreCase(p);
                    }
                    return priority.equalsIgnoreCase(p);
                })
                .filter(n -> {
                    if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) return true;
                    String t = n.getType() != null ? n.getType() : "";
                    if ("CRM".equalsIgnoreCase(type)) {
                        return "CRM".equalsIgnoreCase(t) || "NEW_CONTACT_INQUIRY".equalsIgnoreCase(t) || "CONTACT_INQUIRY".equalsIgnoreCase(t);
                    }
                    if ("INTEGRATION".equalsIgnoreCase(type)) {
                        return t.toUpperCase(Locale.ROOT).contains("INTEGRATION")
                                || t.toUpperCase(Locale.ROOT).contains("GITHUB")
                                || t.toUpperCase(Locale.ROOT).contains("SYNC");
                    }
                    return type.equalsIgnoreCase(t);
                })
                .map(this::toDto)
                .collect(Collectors.toList());

        Page<NotificationDto> filteredPage = new PageImpl<>(filtered, pageable, filtered.size());
        return PageResponse.from(filteredPage);
    }

    @Override
    public NotificationDto markAsRead(UUID id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        n.setRead(true);
        return toDto(notificationRepository.save(n));
    }

    @Override
    public void markAllAsRead() {
        notificationRepository.findAll().forEach(n -> {
            if (!n.isRead()) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Override
    public void deleteNotification(UUID id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", id));
        notificationRepository.delete(n);
    }

    @Override
    @Transactional(readOnly = true)
    public long getUnreadCount() {
        return notificationRepository.countByReadFalse();
    }

    private NotificationDto toDto(Notification entity) {
        NotificationDto dto = new NotificationDto();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setMessage(entity.getMessage());
        dto.setPriority(entity.getPriority());
        dto.setType(entity.getType());
        dto.setRelatedEntityType(entity.getRelatedEntityType());
        dto.setRelatedEntityId(entity.getRelatedEntityId());
        dto.setActionUrl(entity.getActionUrl());
        dto.setRead(entity.isRead());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
