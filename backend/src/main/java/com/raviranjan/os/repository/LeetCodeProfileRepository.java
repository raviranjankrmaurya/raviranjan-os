package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.LeetCodeProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LeetCodeProfileRepository extends JpaRepository<LeetCodeProfile, UUID> {
    Optional<LeetCodeProfile> findByUsername(String username);
}
