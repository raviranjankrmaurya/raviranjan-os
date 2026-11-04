package com.raviranjan.os.service;

import com.raviranjan.os.domain.entity.ContactInquiry;
import com.raviranjan.os.domain.enums.InquiryStatus;
import com.raviranjan.os.dto.crm.ContactInquiryRequest;
import com.raviranjan.os.dto.crm.ContactInquiryResponse;
import com.raviranjan.os.dto.crm.InquiryStatusUpdateRequest;
import com.raviranjan.os.event.Events;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.ContactInquiryMapper;
import com.raviranjan.os.repository.ContactInquiryRepository;
import com.raviranjan.os.repository.NotificationRepository;
import com.raviranjan.os.service.impl.ContactInquiryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContactInquiryServiceTest {

    @Mock
    private ContactInquiryRepository inquiryRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private ContactInquiryMapper mapper;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private ContactInquiryServiceImpl inquiryService;

    private ContactInquiry inquiry;
    private ContactInquiryRequest request;
    private ContactInquiryResponse response;

    @BeforeEach
    void setUp() {
        inquiryService = new ContactInquiryServiceImpl(
                inquiryRepository,
                notificationRepository,
                mapper,
                eventPublisher
        );

        UUID id = UUID.randomUUID();
        inquiry = new ContactInquiry();
        inquiry.setId(id);
        inquiry.setName("Lead Recruiter");
        inquiry.setEmail("recruiter@toptech.com");
        inquiry.setCompany("TopTech");
        inquiry.setMessage("We have an engineering leadership role");
        inquiry.setStatus(InquiryStatus.NEW);

        request = new ContactInquiryRequest();
        request.setName("Lead Recruiter");
        request.setEmail("recruiter@toptech.com");
        request.setCompany("TopTech");
        request.setMessage("We have an engineering leadership role");

        response = new ContactInquiryResponse();
        response.setId(id);
        response.setName("Lead Recruiter");
        response.setStatus(InquiryStatus.NEW);
    }

    @Test
    @DisplayName("Should record inquiry and emit exactly one admin notification event without duplicates")
    void testSubmitInquiry() {
        when(inquiryRepository.save(any(ContactInquiry.class))).thenReturn(inquiry);
        when(mapper.toResponse(inquiry)).thenReturn(response);

        ContactInquiryResponse result = inquiryService.submitInquiry(request, "127.0.0.1", "Mozilla/5.0");

        assertNotNull(result);
        assertEquals(InquiryStatus.NEW, result.getStatus());
        verify(inquiryRepository, times(1)).save(any(ContactInquiry.class));
        verify(eventPublisher, times(1)).publishEvent(any(Events.ContactInquiryEvent.class));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should update triage and moderation status for CRM tracking")
    void testUpdateInquiryStatus() {
        UUID id = inquiry.getId();
        when(inquiryRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(inquiry));
        when(inquiryRepository.save(inquiry)).thenReturn(inquiry);
        when(mapper.toAdminResponse(inquiry)).thenReturn(response);

        InquiryStatusUpdateRequest updateReq = new InquiryStatusUpdateRequest();
        updateReq.setStatus(InquiryStatus.INTERVIEW);
        updateReq.setNotes("Scheduled technical discussion");
        updateReq.setModerationStatus("REVIEWED");

        ContactInquiryResponse result = inquiryService.updateInquiryStatus(id, updateReq);

        assertNotNull(result);
        assertEquals(InquiryStatus.INTERVIEW, inquiry.getStatus());
        assertEquals("Scheduled technical discussion", inquiry.getNotes());
        assertEquals("REVIEWED", inquiry.getModerationStatus());
    }

    @Test
    @DisplayName("Should delete associated notifications when inquiry is deleted")
    void testDeleteInquiryAlsoDeletesNotifications() {
        UUID id = inquiry.getId();
        when(inquiryRepository.findByIdAndDeletedAtIsNull(id)).thenReturn(Optional.of(inquiry));

        inquiryService.deleteInquiry(id);

        verify(notificationRepository, times(1)).deleteByRelatedEntityId(id.toString());
        verify(inquiryRepository, times(1)).delete(inquiry);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when inquiry does not exist")
    void testInquiryNotFound() {
        UUID randomId = UUID.randomUUID();
        when(inquiryRepository.findByIdAndDeletedAtIsNull(randomId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> inquiryService.getInquiryById(randomId));
    }
}
