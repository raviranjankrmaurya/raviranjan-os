package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.Project;
import com.raviranjan.os.domain.enums.ProjectStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID>, JpaSpecificationExecutor<Project> {

    Optional<Project> findBySlug(String slug);

    Optional<Project> findBySlugAndDeletedAtIsNull(String slug);

    Optional<Project> findByIdAndDeletedAtIsNull(UUID id);

    List<Project> findAllByDeletedAtIsNull();

    Page<Project> findAllByDeletedAtIsNull(Pageable pageable);

    Page<Project> findByStatusAndDeletedAtIsNull(ProjectStatus status, Pageable pageable);

    Page<Project> findByStatusAndFeaturedAndDeletedAtIsNull(ProjectStatus status, boolean featured, Pageable pageable);

    Page<Project> findByFeaturedAndDeletedAtIsNull(boolean featured, Pageable pageable);

    boolean existsBySlugAndDeletedAtIsNull(String slug);
}
