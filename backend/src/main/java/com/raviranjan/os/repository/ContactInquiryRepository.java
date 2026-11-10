package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.ContactInquiry;
import com.raviranjan.os.domain.enums.InquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContactInquiryRepository extends JpaRepository<ContactInquiry, UUID> {
    Optional<ContactInquiry> findByIdAndDeletedAtIsNull(UUID id);
    Page<ContactInquiry> findAllByDeletedAtIsNull(Pageable pageable);
    Page<ContactInquiry> findByStatusAndDeletedAtIsNull(InquiryStatus status, Pageable pageable);
    long countByStatusAndDeletedAtIsNull(InquiryStatus status);
    long countByMessageHashAndDeletedAtIsNull(String messageHash);
    long countByIpAddressAndAbusiveTrueAndDeletedAtIsNull(String ipAddress);
    List<ContactInquiry> findByCreatedAtBeforeAndIpAddressIsNotNull(Instant cutoff);
}
