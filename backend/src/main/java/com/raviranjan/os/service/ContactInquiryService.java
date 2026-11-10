package com.raviranjan.os.service;

import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.InquiryStatus;
import com.raviranjan.os.dto.crm.ContactInquiryRequest;
import com.raviranjan.os.dto.crm.ContactInquiryResponse;
import com.raviranjan.os.dto.crm.InquiryStatusUpdateRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface ContactInquiryService {
    ContactInquiryResponse submitInquiry(ContactInquiryRequest request, String ipAddress, String userAgent);
    default ContactInquiryResponse submitInquiry(ContactInquiryRequest request, String ipAddress, String userAgent, String referrer, String browserHeader) {
        return submitInquiry(request, ipAddress, userAgent);
    }
    PageResponse<ContactInquiryResponse> getInquiries(InquiryStatus status, Pageable pageable);
    ContactInquiryResponse getInquiryById(UUID id);
    ContactInquiryResponse updateInquiryStatus(UUID id, InquiryStatusUpdateRequest request);
    void deleteInquiry(UUID id);
    default int applyMetadataRetentionPolicy(int retentionDays) {
        return 0;
    }
}
