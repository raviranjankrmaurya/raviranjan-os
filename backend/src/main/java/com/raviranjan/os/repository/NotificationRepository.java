package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findAllByOrderByCreatedAtDesc(Pageable pageable);
    List<Notification> findByRelatedEntityId(String relatedEntityId);
    void deleteByRelatedEntityId(String relatedEntityId);
    long countByReadFalse();
}
