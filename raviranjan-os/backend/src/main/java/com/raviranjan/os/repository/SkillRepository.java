package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SkillRepository extends JpaRepository<Skill, UUID> {
    List<Skill> findByCategoryIdOrderByDisplayOrderAsc(UUID categoryId);
    List<Skill> findByFeaturedTrueOrderByDisplayOrderAsc();
    Optional<Skill> findByNameIgnoreCase(String name);
    boolean existsByName(String name);
}
