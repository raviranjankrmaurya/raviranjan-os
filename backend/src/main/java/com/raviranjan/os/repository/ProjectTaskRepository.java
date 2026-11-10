package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.ProjectTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProjectTaskRepository extends JpaRepository<ProjectTask, UUID> {
    List<ProjectTask> findByProjectIdOrderByDisplayOrderAsc(UUID projectId);
}
