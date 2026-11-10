package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.ContributionStory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ContributionStoryRepository extends JpaRepository<ContributionStory, UUID> {
    Page<ContributionStory> findAllByOrderByContributionDateDesc(Pageable pageable);
}
