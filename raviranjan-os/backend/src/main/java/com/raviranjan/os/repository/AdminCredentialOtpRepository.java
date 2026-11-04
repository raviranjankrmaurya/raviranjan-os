package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.AdminCredentialOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdminCredentialOtpRepository extends JpaRepository<AdminCredentialOtp, UUID> {
    Optional<AdminCredentialOtp> findFirstByEmailIgnoreCaseAndMobileNumberAndOtpCodeAndUsedFalseOrderByCreatedAtDesc(
            String email, String mobileNumber, String otpCode
    );
    List<AdminCredentialOtp> findTop5ByOrderByCreatedAtDesc();
}
