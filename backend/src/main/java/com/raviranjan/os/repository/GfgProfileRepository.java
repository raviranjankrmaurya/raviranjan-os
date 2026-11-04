package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.GfgProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface GfgProfileRepository extends JpaRepository<GfgProfile, UUID> {
    Optional<GfgProfile> findByUsername(String username);
}
