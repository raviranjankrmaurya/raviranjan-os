package com.raviranjan.os.mapper;

import com.raviranjan.os.domain.entity.ContactInquiry;
import com.raviranjan.os.dto.crm.ContactInquiryResponse;
import org.springframework.stereotype.Component;

@Component
public class ContactInquiryMapper {

    /**
     * Public-safe response mapper: deliberately omits IP address, User-Agent, Browser Hash,
     * Geolocation coordinates, and internal security moderation metadata.
     */
    public ContactInquiryResponse toResponse(ContactInquiry entity) {
        if (entity == null) return null;
        ContactInquiryResponse res = new ContactInquiryResponse();
        res.setId(entity.getId());
        res.setName(entity.getName());
        res.setEmail(entity.getEmail());
        res.setCompany(entity.getCompany());
        res.setSubject(entity.getSubject());
        res.setMessage(entity.getMessage());
        res.setStatus(entity.getStatus());
        res.setCreatedAt(entity.getCreatedAt());
        return res;
    }

    /**
     * Authorized Admin CRM response mapper: includes full Contact Intelligence, GeoIP,
     * Repeat Abuse signals, and AI Moderation metadata.
     */
    public ContactInquiryResponse toAdminResponse(ContactInquiry entity) {
        if (entity == null) return null;
        ContactInquiryResponse res = toResponse(entity);
        res.setNotes(entity.getNotes());
        res.setIpAddress(entity.getIpAddress());
        res.setCountry(entity.getCountry());
        res.setRegion(entity.getRegion());
        res.setCity(entity.getCity());
        res.setLatitude(entity.getLatitude());
        res.setLongitude(entity.getLongitude());
        res.setGeoProvider(entity.getGeoProvider());
        res.setUserAgent(entity.getUserAgent());
        res.setReferrer(entity.getReferrer());
        res.setBrowserHash(entity.getBrowserHash());
        res.setDisposableEmail(entity.isDisposableEmail());
        res.setRepeatSubmission(entity.isRepeatSubmission());
        res.setRepeatCount(entity.getRepeatCount());
        res.setIntent(entity.getIntent());
        res.setSpam(entity.isSpam());
        res.setAbusive(entity.isAbusive());
        res.setThreatening(entity.isThreatening());
        res.setModerationConfidence(entity.getModerationConfidence());
        res.setModerationReason(entity.getModerationReason());
        res.setRecommendedAction(entity.getRecommendedAction());
        res.setModerationStatus(entity.getModerationStatus());
        return res;
    }
}
