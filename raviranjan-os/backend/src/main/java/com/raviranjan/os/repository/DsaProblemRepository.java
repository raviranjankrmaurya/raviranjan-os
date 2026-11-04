package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.DsaProblem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DsaProblemRepository extends JpaRepository<DsaProblem, UUID> {
    Optional<DsaProblem> findByPlatformAndSlug(String platform, String slug);
    Page<DsaProblem> findByPlatform(String platform, Pageable pageable);
}
