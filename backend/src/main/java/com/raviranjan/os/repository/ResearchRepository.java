package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.Research;
import com.raviranjan.os.domain.enums.ResearchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResearchRepository extends JpaRepository<Research, UUID> {
    Optional<Research> findBySlug(String slug);
    Page<Research> findByStatus(ResearchStatus status, Pageable pageable);
    boolean existsBySlug(String slug);
}
