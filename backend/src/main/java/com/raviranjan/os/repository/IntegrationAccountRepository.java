package com.raviranjan.os.repository;

import com.raviranjan.os.domain.entity.IntegrationAccount;
import com.raviranjan.os.domain.enums.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface IntegrationAccountRepository extends JpaRepository<IntegrationAccount, UUID> {
    Optional<IntegrationAccount> findByProvider(ProviderType provider);
}
