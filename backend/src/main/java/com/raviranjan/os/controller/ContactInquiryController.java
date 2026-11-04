package com.raviranjan.os.controller;

import com.raviranjan.os.common.ApiResponse;
import com.raviranjan.os.common.AppConstants;
import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.enums.InquiryStatus;
import com.raviranjan.os.dto.crm.ContactInquiryRequest;
import com.raviranjan.os.dto.crm.ContactInquiryResponse;
import com.raviranjan.os.dto.crm.InquiryStatusUpdateRequest;
import com.raviranjan.os.security.ClientIpResolver;
import com.raviranjan.os.service.ContactInquiryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@Tag(name = "Contact & CRM", description = "Recruiter inquiries, direct contact submissions, and intelligence ingestion")
public class ContactInquiryController {

    private final ContactInquiryService inquiryService;
    private final ClientIpResolver clientIpResolver;

    public ContactInquiryController(ContactInquiryService inquiryService) {
        this(inquiryService, new ClientIpResolver(true, ""));
    }

    @Autowired
    public ContactInquiryController(ContactInquiryService inquiryService, ClientIpResolver clientIpResolver) {
        this.inquiryService = inquiryService;
        this.clientIpResolver = clientIpResolver;
    }

    @PostMapping("/api/contact")
    @Operation(summary = "Submit contact inquiry (Public with 3/10m IP rate-limiting and AI moderation)")
    public ResponseEntity<ApiResponse<ContactInquiryResponse>> submitContact(
            @Valid @RequestBody ContactInquiryRequest request,
            HttpServletRequest httpServletRequest
    ) {
        String clientIp = clientIpResolver.resolveClientIp(httpServletRequest);
        String visitorPublicIp = httpServletRequest.getHeader("X-Visitor-Public-Ip");

        if (request.getClientIp() != null && !request.getClientIp().isBlank() && !clientIpResolver.isPrivateOrLoopback(request.getClientIp())) {
            clientIp = request.getClientIp().trim();
        } else if ((clientIp == null || clientIpResolver.isPrivateOrLoopback(clientIp)) && visitorPublicIp != null && !visitorPublicIp.isBlank()) {
            String normalizedVisitorIp = clientIpResolver.normalizeIp(visitorPublicIp);
            if (normalizedVisitorIp != null && !clientIpResolver.isPrivateOrLoopback(normalizedVisitorIp)) {
                clientIp = normalizedVisitorIp;
            }
        }

        String userAgent = httpServletRequest.getHeader("User-Agent");
        String referrer = httpServletRequest.getHeader("Referer");
        String browserSignal = httpServletRequest.getHeader("X-Browser-Signal");
        String hintCity = httpServletRequest.getHeader("X-Visitor-City");
        String hintRegion = httpServletRequest.getHeader("X-Visitor-Region");
        String hintCountry = httpServletRequest.getHeader("X-Visitor-Country");
        String hintTz = httpServletRequest.getHeader("X-Visitor-Timezone");

        if (request.getCity() == null || request.getCity().isBlank()) {
            request.setCity(hintCity);
        }
        if (request.getRegion() == null || request.getRegion().isBlank()) {
            request.setRegion(hintRegion);
        }
        if (request.getCountry() == null || request.getCountry().isBlank()) {
            request.setCountry(hintCountry);
        }
        if (request.getBrowserHash() == null || request.getBrowserHash().isBlank()) {
            String bHashHdr = httpServletRequest.getHeader("X-Browser-Hash");
            if (bHashHdr != null && !bHashHdr.isBlank()) request.setBrowserHash(bHashHdr);
        }
        if (request.getOrigin() == null || request.getOrigin().isBlank()) {
            String originHdr = httpServletRequest.getHeader("Origin");
            request.setOrigin(originHdr != null && !originHdr.isBlank() ? originHdr : referrer);
        }
        if (request.getReferrer() == null || request.getReferrer().isBlank()) {
            request.setReferrer(referrer != null && !referrer.isBlank() ? referrer : request.getOrigin());
        }

        String packedGeoMeta = String.join("|",
                visitorPublicIp != null ? visitorPublicIp : (request.getClientIp() != null ? request.getClientIp() : ""),
                request.getCity() != null ? request.getCity() : "",
                request.getRegion() != null ? request.getRegion() : "",
                request.getCountry() != null ? request.getCountry() : "",
                hintTz != null ? hintTz : ""
        );

        ContactInquiryResponse response = inquiryService.submitInquiry(
                request,
                clientIp,
                userAgent,
                packedGeoMeta,
                browserSignal
        );
        return new ResponseEntity<>(
                ApiResponse.success("Thank you for reaching out. Your message has been received.", response),
                HttpStatus.CREATED
        );
    }

    @GetMapping("/api/admin/inquiries")
    @Operation(summary = "List inquiries with full Contact Intelligence & AI Moderation metadata (Admin)")
    public ResponseEntity<ApiResponse<PageResponse<ContactInquiryResponse>>> getInquiries(
            @RequestParam(required = false) InquiryStatus status,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
            @RequestParam(defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getInquiries(status, pageable)));
    }

    @GetMapping("/api/admin/inquiries/{id}")
    @Operation(summary = "Get inquiry by ID with full moderation metadata (Admin)")
    public ResponseEntity<ApiResponse<ContactInquiryResponse>> getInquiryById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.getInquiryById(id)));
    }

    @RequestMapping(value = "/api/admin/inquiries/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "Update inquiry triage or moderation status (Admin: Reviewed, False Positive, Confirmed Spam)")
    public ResponseEntity<ApiResponse<ContactInquiryResponse>> updateInquiryStatus(
            @PathVariable UUID id,
            @RequestBody InquiryStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(inquiryService.updateInquiryStatus(id, request)));
    }

    @DeleteMapping("/api/admin/inquiries/{id}")
    @Operation(summary = "Delete an inquiry record and purge its orphaned notification (Admin)")
    public ResponseEntity<ApiResponse<Void>> deleteInquiry(@PathVariable UUID id) {
        inquiryService.deleteInquiry(id);
        return ResponseEntity.ok(ApiResponse.success("Inquiry and associated notification successfully deleted", null));
    }

    @PostMapping("/api/admin/inquiries/retention-cleanup")
    @Operation(summary = "Execute Privacy Retention Cleanup on IP & User-Agent metadata (Admin)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> runRetentionCleanup(
            @RequestParam(defaultValue = "90") int retentionDays
    ) {
        int scrubbed = inquiryService.applyMetadataRetentionPolicy(retentionDays);
        return ResponseEntity.ok(ApiResponse.success("Retention policy executed", Map.of(
                "retentionDays", retentionDays,
                "recordsScrubbed", scrubbed
        )));
    }
}
