package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CertificationRepository extends JpaRepository<Certification, UUID> {
    List<Certification> findAllByOrderByIssueDateDesc();
    List<Certification> findByFeaturedTrueOrderByIssueDateDesc();
}
