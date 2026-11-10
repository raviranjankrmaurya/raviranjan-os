package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.ResearchTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResearchTagRepository extends JpaRepository<ResearchTag, UUID> {
    Optional<ResearchTag> findBySlug(String slug);
}
