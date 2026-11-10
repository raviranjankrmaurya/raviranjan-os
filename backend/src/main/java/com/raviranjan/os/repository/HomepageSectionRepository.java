package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.HomepageSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface HomepageSectionRepository extends JpaRepository<HomepageSection, UUID> {
    List<HomepageSection> findByVisibleTrueOrderByDisplayOrderAsc();
}
