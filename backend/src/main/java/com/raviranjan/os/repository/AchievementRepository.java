package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AchievementRepository extends JpaRepository<Achievement, UUID> {
    List<Achievement> findAllByOrderByDateDesc();
    List<Achievement> findByFeaturedTrueOrderByDateDesc();
}
