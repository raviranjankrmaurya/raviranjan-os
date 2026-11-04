package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.common.AppConstants;
import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.dto.notification.NotificationDto;
import com.raviranjan.os.realtime.AdminSseEmitterRegistry;
import com.raviranjan.os.service.NotificationCenterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/notifications")
@Tag(name = "Admin Notification Center", description = "Real-time alerts, priority triage, and system event feeds")
public class NotificationController {

    private final NotificationCenterService notificationService;
    private final AdminSseEmitterRegistry adminSseRegistry;

    public NotificationController(
            NotificationCenterService notificationService,
            AdminSseEmitterRegistry adminSseRegistry
    ) {
        this.notificationService = notificationService;
        this.adminSseRegistry = adminSseRegistry;
    }

    @GetMapping
    @Operation(summary = "Get notifications with priority and read-status filtering (Admin)")
    public ResponseEntity<ApiResponse<PageResponse<NotificationDto>>> getNotifications(
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.success(notificationService.getNotifications(isRead, priority, type, pageable)));
    }

    @RequestMapping(value = "/{id}/read", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Mark single notification as read (Admin)")
    public ResponseEntity<ApiResponse<NotificationDto>> markRead(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Notification marked as read", notificationService.markAsRead(id)));
    }

    @RequestMapping(value = "/read-all", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Mark all notifications as read (Admin)")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(ApiResponse.success("All notifications marked as read", null));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification from Neon DB (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(@PathVariable UUID id) {
        notificationService.deleteNotification(id);
        return ResponseEntity.ok(ApiResponse.success("Notification deleted", null));
    }

    @GetMapping("/count-unread")
    @Operation(summary = "Get count of unread notifications for badge (Admin)")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", notificationService.getUnreadCount())));
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Connect to live Admin Server-Sent Events (SSE) notification stream")
    public SseEmitter subscribeToAdminStream() {
        return adminSseRegistry.register();
    }
}
