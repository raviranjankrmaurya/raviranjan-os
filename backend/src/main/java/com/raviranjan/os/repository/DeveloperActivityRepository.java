package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.DeveloperActivity;
import com.raviranjan.os.domain.enums.ActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeveloperActivityRepository extends JpaRepository<DeveloperActivity, UUID> {
    Page<DeveloperActivity> findAllByOrderByOccurredAtDesc(Pageable pageable);
    Page<DeveloperActivity> findByActivityTypeOrderByOccurredAtDesc(ActivityType type, Pageable pageable);
    Page<DeveloperActivity> findBySourcePlatformOrderByOccurredAtDesc(String sourcePlatform, Pageable pageable);
    Optional<DeveloperActivity> findBySourcePlatformAndExternalId(String sourcePlatform, String externalId);
    boolean existsBySourcePlatformAndExternalId(String sourcePlatform, String externalId);
}
