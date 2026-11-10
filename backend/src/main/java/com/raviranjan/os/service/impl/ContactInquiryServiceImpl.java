package com.raviranjan.os.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.raviranjan.os.common.PageResponse;
import com.raviranjan.os.domain.entity.ContactInquiry;
import com.raviranjan.os.domain.entity.Notification;
import com.raviranjan.os.domain.enums.InquiryStatus;
import com.raviranjan.os.dto.crm.ContactInquiryRequest;
import com.raviranjan.os.dto.crm.ContactInquiryResponse;
import com.raviranjan.os.dto.crm.InquiryStatusUpdateRequest;
import com.raviranjan.os.event.Events;
import com.raviranjan.os.exception.ResourceNotFoundException;
import com.raviranjan.os.mapper.ContactInquiryMapper;
import com.raviranjan.os.repository.ContactInquiryRepository;
import com.raviranjan.os.repository.NotificationRepository;
import com.raviranjan.os.security.ClientIpResolver;
import com.raviranjan.os.service.ContactInquiryService;
import com.raviranjan.os.service.security.ContactModerationService;
import com.raviranjan.os.service.security.ContactRateLimiter;
import com.raviranjan.os.service.security.ContactValidator;
import com.raviranjan.os.service.security.GeoIpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ContactInquiryServiceImpl implements ContactInquiryService {

    private static final Logger log = LoggerFactory.getLogger(ContactInquiryServiceImpl.class);

    private final ContactInquiryRepository inquiryRepository;
    private final NotificationRepository notificationRepository;
    private final ContactInquiryMapper mapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ContactValidator contactValidator;
    private final ContactRateLimiter rateLimiter;
    private final GeoIpService geoIpService;
    private final ContactModerationService moderationService;

    /**
     * Backward-compatible constructor for existing unit tests.
     */
    public ContactInquiryServiceImpl(
            ContactInquiryRepository inquiryRepository,
            NotificationRepository notificationRepository,
            ContactInquiryMapper mapper,
            ApplicationEventPublisher eventPublisher
    ) {
        this(
                inquiryRepository,
                notificationRepository,
                mapper,
                eventPublisher,
                new ContactValidator(),
                new ContactRateLimiter(null, 3, 600),
                new GeoIpService(new ClientIpResolver(true, ""), new ObjectMapper(), false),
                new ContactModerationService(null, new ObjectMapper(), "")
        );
    }

    @Autowired
    public ContactInquiryServiceImpl(
            ContactInquiryRepository inquiryRepository,
            NotificationRepository notificationRepository,
            ContactInquiryMapper mapper,
            ApplicationEventPublisher eventPublisher,
            ContactValidator contactValidator,
            ContactRateLimiter rateLimiter,
            GeoIpService geoIpService,
            ContactModerationService moderationService
    ) {
        this.inquiryRepository = inquiryRepository;
        this.notificationRepository = notificationRepository;
        this.mapper = mapper;
        this.eventPublisher = eventPublisher;
        this.contactValidator = contactValidator;
        this.rateLimiter = rateLimiter;
        this.geoIpService = geoIpService;
        this.moderationService = moderationService;
    }

    @Override
    public ContactInquiryResponse submitInquiry(ContactInquiryRequest request, String ipAddress, String userAgent) {
        return submitInquiry(request, ipAddress, userAgent, null, null);
    }

    @Override
    public ContactInquiryResponse submitInquiry(
            ContactInquiryRequest request,
            String ipAddress,
            String userAgent,
            String referrer,
            String browserHeader
    ) {
        // 1. Validate & normalize input FIRST (malformed requests rejected here NEVER increment the rate-limit counter)
        ContactValidator.ValidatedContactInput validated = contactValidator.validateAndNormalize(request);

        // 2. Enforce atomic backend rate limit (3 submissions per rolling 10-minute window per client IP)
        rateLimiter.checkAndIncrement(ipAddress);

        // 3. Approximate GeoIP resolution (resolves public IP, browser Geo headers, or timezone fallback)
        GeoIpService.GeoLocation geo = GeoIpService.GeoLocation.unresolved("SKIPPED");
        try {
            String hintIp = null, hintCity = null, hintRegion = null, hintCountry = null, hintTz = null;
            if (referrer != null && referrer.contains("|")) {
                String[] parts = referrer.split("\\|", -1);
                if (parts.length >= 5) {
                    hintIp = parts[0];
                    hintCity = parts[1];
                    hintRegion = parts[2];
                    hintCountry = parts[3];
                    hintTz = parts[4];
                    referrer = null;
                }
            }
            geo = geoIpService.resolveInquiryLocation(ipAddress, hintIp, hintCity, hintRegion, hintCountry, hintTz);
        } catch (Exception e) {
            log.debug("GeoIP lookup skipped: {}", e.getMessage());
        }

        // 4. Repeat-submission & repeat-abuse correlation
        long identicalPriorCount = 0;
        long priorAbuseFromIp = 0;
        try {
            identicalPriorCount = inquiryRepository.countByMessageHashAndDeletedAtIsNull(validated.messageSha256());
            if (ipAddress != null && !ipAddress.isBlank()) {
                priorAbuseFromIp = inquiryRepository.countByIpAddressAndAbusiveTrueAndDeletedAtIsNull(ipAddress);
            }
        } catch (Exception ignored) {}

        boolean isRepeat = identicalPriorCount > 0;
        int totalRepeatCount = (int) identicalPriorCount + 1;

        // 5. AI + Deterministic Content Moderation (never sends IP address to external AI provider)
        ContactModerationService.ModerationResult mod = moderationService.moderateSubmission(
                validated.normalizedName(),
                (validated.sanitizedCompany() != null ? validated.sanitizedCompany() + " — " : "") + validated.sanitizedSubject(),
                validated.originalMessage(),
                validated.disposableEmailDomain(),
                isRepeat,
                totalRepeatCount
        );

        String browserHash = (request.getBrowserHash() != null && !request.getBrowserHash().isBlank())
                ? request.getBrowserHash()
                : null;
        if (browserHash == null) {
            String rawBrowserSignal = browserHeader != null && !browserHeader.isBlank()
                    ? browserHeader
                    : request.getBrowserIdentifier();
            browserHash = contactValidator.hashBrowserIdentifier(rawBrowserSignal, userAgent);
        }

        // 6. Populate & save ContactInquiry entity
        ContactInquiry inquiry = new ContactInquiry();
        inquiry.setName(validated.normalizedName());
        inquiry.setEmail(validated.normalizedEmail());
        inquiry.setCompany(validated.sanitizedCompany());
        inquiry.setSubject(validated.sanitizedSubject());
        inquiry.setMessage(validated.originalMessage());
        inquiry.setStatus(mod.isSpam() && mod.confidence() >= 0.85 ? InquiryStatus.REJECTED : InquiryStatus.NEW);
        inquiry.setIpAddress(ipAddress);

        String finalCountry = (geo.country() != null && !geo.country().isBlank() && !"Location Unavailable".equalsIgnoreCase(geo.country()))
                ? geo.country()
                : (request.getCountry() != null && !request.getCountry().isBlank() ? request.getCountry() : "India");
        String finalCity = (geo.city() != null && !geo.city().isBlank() && !"Location Unavailable".equalsIgnoreCase(geo.city()))
                ? geo.city()
                : (request.getCity() != null ? request.getCity() : "New Delhi");
        String finalRegion = (geo.region() != null && !geo.region().isBlank() && !"Location Unavailable".equalsIgnoreCase(geo.region()))
                ? geo.region()
                : (request.getRegion() != null ? request.getRegion() : "Delhi NCR / Bihar");

        inquiry.setCountry(finalCountry);
        inquiry.setRegion(finalRegion);
        inquiry.setCity(finalCity);
        inquiry.setLatitude(geo.latitude());
        inquiry.setLongitude(geo.longitude());
        inquiry.setGeoProvider(geo.provider() != null ? geo.provider() : "Regional-GeoIP");
        inquiry.setUserAgent(userAgent != null && userAgent.length() > 512 ? userAgent.substring(0, 512) : userAgent);
        String finalReferrer = (request.getReferrer() != null && !request.getReferrer().isBlank())
                ? request.getReferrer()
                : (referrer != null && !referrer.contains("|") ? referrer : (request.getOrigin() != null ? request.getOrigin() : "https://raviranjan.dev"));
        inquiry.setReferrer(finalReferrer != null && finalReferrer.length() > 512 ? finalReferrer.substring(0, 512) : finalReferrer);
        inquiry.setBrowserHash(browserHash);
        inquiry.setMessageHash(validated.messageSha256());
        inquiry.setDisposableEmail(validated.disposableEmailDomain());
        inquiry.setRepeatSubmission(isRepeat);
        inquiry.setRepeatCount(totalRepeatCount);
        inquiry.setIntent(mod.intent());
        inquiry.setSpam(mod.isSpam());
        inquiry.setAbusive(mod.isAbusive());
        inquiry.setThreatening(mod.isThreatening());
        inquiry.setModerationConfidence(mod.confidence());
        inquiry.setModerationReason(mod.reason());
        inquiry.setRecommendedAction(mod.recommendedAction());
        inquiry.setModerationStatus(mod.moderationStatus());

        ContactInquiry saved = inquiryRepository.save(inquiry);

        // 7. Emit exactly ONE escaped notification event (eliminating duplicate notifications)
        String safeName = contactValidator.escapeHtml(saved.getName());
        String safeEmail = contactValidator.escapeHtml(saved.getEmail());
        String safeMsg = contactValidator.escapeHtml(saved.getMessage());
        if (safeMsg.length() > 240) {
            safeMsg = safeMsg.substring(0, 240) + "...";
        }

        String locationBadge = (geo.city() != null && geo.country() != null)
                ? " [" + contactValidator.escapeHtml(geo.city() + ", " + geo.country()) + "]"
                : "";

        String notifTitle;
        if (mod.aiProviderFailed()) {
            notifTitle = "[AI Moderation Attention] Inquiry from " + safeName + " <" + safeEmail + ">";
        } else if (mod.isThreatening() || (mod.isAbusive() && (isRepeat || priorAbuseFromIp > 0))) {
            notifTitle = "[Repeated Abuse Alert] " + safeName + " <" + safeEmail + ">";
        } else if (mod.isSpam() && mod.confidence() >= 0.80) {
            notifTitle = "[High-Confidence Spam] " + safeName + " <" + safeEmail + ">";
        } else {
            notifTitle = "[" + mod.intent() + "] " + safeName + " <" + safeEmail + ">" + locationBadge;
        }

        String notifBody = safeMsg + " — Moderation: " + mod.moderationStatus() + " (" + contactValidator.escapeHtml(mod.reason()) + ")";
        String inquiryIdStr = saved.getId() != null ? saved.getId().toString() : UUID.randomUUID().toString();

        eventPublisher.publishEvent(new Events.ContactInquiryEvent(
                this,
                notifTitle,
                notifBody,
                inquiryIdStr,
                "#crm"
        ));

        // 8. Return public-safe DTO (never exposes IP, User-Agent, or internal moderation metadata)
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ContactInquiryResponse> getInquiries(InquiryStatus status, Pageable pageable) {
        Page<ContactInquiry> page = (status == null)
                ? inquiryRepository.findAllByDeletedAtIsNull(pageable)
                : inquiryRepository.findByStatusAndDeletedAtIsNull(status, pageable);

        return PageResponse.from(page.map(mapper::toAdminResponse));
    }

    @Override
    @Transactional(readOnly = true)
    public ContactInquiryResponse getInquiryById(UUID id) {
        ContactInquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContactInquiry", "id", id));
        return mapper.toAdminResponse(inquiry);
    }

    @Override
    public ContactInquiryResponse updateInquiryStatus(UUID id, InquiryStatusUpdateRequest request) {
        ContactInquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContactInquiry", "id", id));

        if (request.getStatus() != null) {
            inquiry.setStatus(request.getStatus());
        }
        if (request.getNotes() != null) {
            inquiry.setNotes(request.getNotes());
        }
        if (request.getModerationStatus() != null && !request.getModerationStatus().isBlank()) {
            String modStatus = request.getModerationStatus().trim().toUpperCase();
            inquiry.setModerationStatus(modStatus);
            if ("FALSE_POSITIVE".equals(modStatus)) {
                inquiry.setSpam(false);
                inquiry.setAbusive(false);
                inquiry.setThreatening(false);
                if ("SPAM".equals(inquiry.getIntent()) || "ABUSIVE_LANGUAGE".equals(inquiry.getIntent())) {
                    inquiry.setIntent("GENUINE_INQUIRY");
                }
                if (inquiry.getStatus() == InquiryStatus.REJECTED) {
                    inquiry.setStatus(InquiryStatus.NEW);
                }
            } else if ("CONFIRMED_SPAM".equals(modStatus)) {
                inquiry.setSpam(true);
                inquiry.setIntent("SPAM");
                inquiry.setStatus(InquiryStatus.REJECTED);
            }
        }
        if (request.getSpam() != null) {
            inquiry.setSpam(request.getSpam());
        }
        if (request.getAbusive() != null) {
            inquiry.setAbusive(request.getAbusive());
        }
        if (request.getIntent() != null && !request.getIntent().isBlank()) {
            inquiry.setIntent(request.getIntent().trim().toUpperCase());
        }
        if (request.getModerationReason() != null && !request.getModerationReason().isBlank()) {
            inquiry.setModerationReason(request.getModerationReason().trim());
        }

        ContactInquiry saved = inquiryRepository.save(inquiry);
        return mapper.toAdminResponse(saved);
    }

    @Override
    public void deleteInquiry(UUID id) {
        ContactInquiry inquiry = inquiryRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResourceNotFoundException("ContactInquiry", "id", id));

        // Automatically delete linked notification(s) in Notification Center when Admin deletes a CRM message
        try {
            notificationRepository.deleteByRelatedEntityId(id.toString());
            List<Notification> allNotifs = notificationRepository.findAll();
            for (Notification n : allNotifs) {
                boolean matchesId = id.toString().equals(n.getRelatedEntityId());
                boolean matchesLegacyText = (n.getTitle() != null && inquiry.getEmail() != null && n.getTitle().contains(inquiry.getEmail()))
                        || (n.getTitle() != null && inquiry.getName() != null && n.getTitle().equals(inquiry.getName()) && "NEW_CONTACT_INQUIRY".equals(n.getType()));
                if (matchesId || matchesLegacyText) {
                    notificationRepository.delete(n);
                }
            }
        } catch (Exception e) {
            log.warn("Notification cleanup warning on inquiry delete: {}", e.getMessage());
        }

        inquiryRepository.delete(inquiry);
    }

    @Override
    public int applyMetadataRetentionPolicy(int retentionDays) {
        int safeDays = Math.max(1, retentionDays);
        Instant cutoff = Instant.now().minus(Duration.ofDays(safeDays));
        List<ContactInquiry> expired = inquiryRepository.findByCreatedAtBeforeAndIpAddressIsNotNull(cutoff);
        int scrubbed = 0;
        for (ContactInquiry inq : expired) {
            inq.setIpAddress(null);
            inq.setUserAgent(null);
            inq.setBrowserHash(null);
            inq.setLatitude(null);
            inq.setLongitude(null);
            inquiryRepository.save(inq);
            scrubbed++;
        }
        return scrubbed;
    }
}
