package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.IntegrationSyncLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface IntegrationSyncLogRepository extends JpaRepository<IntegrationSyncLog, UUID> {
    Page<IntegrationSyncLog> findByAccountIdOrderByCreatedAtDesc(UUID accountId, Pageable pageable);
}
