package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.SiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiteSettingRepository extends JpaRepository<SiteSetting, UUID> {
    Optional<SiteSetting> findBySettingKey(String settingKey);
    List<SiteSetting> findByIsPublicTrue();
}
