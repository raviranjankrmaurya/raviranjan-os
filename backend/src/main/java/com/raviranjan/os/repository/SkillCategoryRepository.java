package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.SkillCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillCategoryRepository extends JpaRepository<SkillCategory, UUID> {
    List<SkillCategory> findAllByOrderByDisplayOrderAsc();
    Optional<SkillCategory> findBySlug(String slug);
    boolean existsBySlug(String slug);
}
