package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.LabExperiment;
import com.raviranjan.os.domain.enums.LabStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LabExperimentRepository extends JpaRepository<LabExperiment, UUID> {
    Optional<LabExperiment> findBySlug(String slug);
    Page<LabExperiment> findByStatus(LabStatus status, Pageable pageable);
    boolean existsBySlug(String slug);
}
