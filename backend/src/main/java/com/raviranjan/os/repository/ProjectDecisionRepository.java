package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.ProjectDecision;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectDecisionRepository extends JpaRepository<ProjectDecision, UUID> {
    List<ProjectDecision> findByProjectIdOrderByDecidedAtDesc(UUID projectId);
}
