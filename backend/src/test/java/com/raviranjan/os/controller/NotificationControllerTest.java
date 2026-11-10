package com.raviranjan.os.controller;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.dto.notification.NotificationDto;
import com.raviranjan.os.realtime.AdminSseEmitterRegistry;
import com.raviranjan.os.security.CustomUserDetailsService;
import com.raviranjan.os.security.JwtTokenProvider;
import com.raviranjan.os.service.NotificationCenterService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NotificationCenterService notificationService;
    @MockBean
    private AdminSseEmitterRegistry adminSseRegistry;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /api/admin/notifications should return list of notifications")
    void testGetNotifications() throws Exception {
        NotificationDto dto = new NotificationDto();
        dto.setId(UUID.randomUUID());
        dto.setTitle("New Inquiry");
        dto.setMessage("Message content");
        dto.setPriority("URGENT");
        dto.setType("CRM");
        dto.setRead(false);
        dto.setCreatedAt(Instant.now());

        PageResponse<NotificationDto> page = new PageResponse<>(List.of(dto), 0, 10, 1, 1, true);
        when(notificationService.getNotifications(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get("/api/admin/notifications")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("New Inquiry"));
    }

    @Test
    @DisplayName("GET /api/admin/notifications/stream should return SSE emitter")
    void testSubscribeAdminStream() throws Exception {
        when(adminSseRegistry.register()).thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/admin/notifications/stream"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.TEXT_EVENT_STREAM_VALUE));
    }
}
